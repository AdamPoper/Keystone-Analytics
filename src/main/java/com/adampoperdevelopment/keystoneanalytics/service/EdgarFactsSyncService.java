package com.adampoperdevelopment.keystoneanalytics.service;

import com.adampoperdevelopment.keystoneanalytics.dto.CompanyFactsResponseDto;
import com.adampoperdevelopment.keystoneanalytics.dto.FactEntryDto;
import com.adampoperdevelopment.keystoneanalytics.dto.XbrlTagDto;
import com.adampoperdevelopment.keystoneanalytics.entity.Company;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRowEntry;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRowTag;
import com.adampoperdevelopment.keystoneanalytics.repository.CompanyRepository;
import com.adampoperdevelopment.keystoneanalytics.repository.FinancialStatementRowEntryRepository;
import com.adampoperdevelopment.keystoneanalytics.repository.FinancialStatementRowRepository;
import com.adampoperdevelopment.keystoneanalytics.repository.FinancialStatementRowTagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EdgarFactsSyncService {

    private static final Logger log = LoggerFactory.getLogger(EdgarFactsSyncService.class);
    private static final int APPLE_CIK = 320193;
    private static final Pattern FRAME_PATTERN = Pattern.compile("CY(\\d{4})(Q[1-4])?");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final FinancialStatementRowRepository rowRepository;
    private final FinancialStatementRowTagRepository tagRepository;
    private final FinancialStatementRowEntryRepository entryRepository;

    public EdgarFactsSyncService(RestTemplate restTemplate,
                                 ObjectMapper objectMapper,
                                 CompanyRepository companyRepository,
                                 FinancialStatementRowRepository rowRepository,
                                 FinancialStatementRowTagRepository tagRepository,
                                 FinancialStatementRowEntryRepository entryRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.companyRepository = companyRepository;
        this.rowRepository = rowRepository;
        this.tagRepository = tagRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void sync() {
        Company apple = companyRepository.findByCik(APPLE_CIK);
        if (apple == null) {
            log.warn("Apple (CIK {}) not found in DB — run EdgarSyncService first.", APPLE_CIK);
            return;
        }

        log.info("Fetching company facts for Apple (CIK: {})...", APPLE_CIK);
        String json = fetchCompanyFacts(APPLE_CIK);
        if (json == null) return;

        CompanyFactsResponseDto response;
        try {
            response = objectMapper.readValue(json, CompanyFactsResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to parse company facts response", e);
            return;
        }

        syncTerm(apple, "Revenue", response);
    }

    private void syncTerm(Company company, String rowTitle, CompanyFactsResponseDto response) {
        FinancialStatementRow row = rowRepository.findByTitle(rowTitle);
        if (row == null) {
            log.warn("'{}' row not found — run FinancialStatementRowSeedService first.", rowTitle);
            return;
        }

        if (entryRepository.existsByCompanyAndFinancialStatementRow(company, row)) {
            log.info("{} entries for {} already exist, skipping.", rowTitle, company.getName());
            return;
        }

        List<FinancialStatementRowTag> tags = tagRepository.findByFinancialStatementRowOrderByPriorityAsc(row);
        if (tags.isEmpty()) {
            log.warn("No XBRL tags configured for '{}'", rowTitle);
            return;
        }

        // Try each tag in priority order. For any given frame, the first tag that
        // provides data wins — this naturally covers the pre/post ASC 606 revenue split.
        Map<String, FactEntryDto> byFrame = new LinkedHashMap<>();
        for (FinancialStatementRowTag tagDef : tags) {
            XbrlTagDto tagData = response.getFacts().getUsGaap().get(tagDef.getTag());
            if (tagData == null) {
                log.info("  Tag '{}': not present in company facts", tagDef.getTag());
                continue;
            }

            List<FactEntryDto> entries = tagData.getUnits().get("USD");
            if (entries == null) continue;

            int added = 0;
            for (FactEntryDto e : entries) {
                if (e.getFrame() != null && !byFrame.containsKey(e.getFrame())) {
                    byFrame.put(e.getFrame(), e);
                    added++;
                }
            }
            log.info("  Tag '{}': {} frames added", tagDef.getTag(), added);
        }

        log.info("{} total framed entries for '{}'", byFrame.size(), rowTitle);

        deriveMissingFiscalQ4s(byFrame);

        List<FinancialStatementRowEntry> toSave = new ArrayList<>();
        for (Map.Entry<String, FactEntryDto> e : byFrame.entrySet()) {
            String[] yearPeriod = parseFrame(e.getKey());
            if (yearPeriod == null) continue;

            FinancialStatementRowEntry entry = new FinancialStatementRowEntry();
            entry.setCompany(company);
            entry.setFinancialStatementRow(row);
            entry.setFiscalYear(yearPeriod[0]);
            entry.setFiscalPeriod(yearPeriod[1]);
            entry.setValue(e.getValue().getVal());
            entry.setReportDate(e.getValue().getEnd());
            entry.setCreatedAt(System.currentTimeMillis());
            toSave.add(entry);
        }

        entryRepository.saveAll(toSave);
        log.info("Saved {} '{}' entries for {} ({} derived fiscal Q4s)",
                toSave.size(), rowTitle, company.getName(), countDerived(byFrame));
    }

    private void deriveMissingFiscalQ4s(Map<String, FactEntryDto> byFrame) {
        for (String frame : new ArrayList<>(byFrame.keySet())) {
            if (!frame.matches("CY\\d{4}")) continue;

            FactEntryDto annual = byFrame.get(frame);
            if (annual.getStart() == null || annual.getEnd() == null) continue;

            List<FactEntryDto> quarters = byFrame.values().stream()
                    .filter(e -> e.getFrame() != null && e.getFrame().matches("CY\\d{4}Q[1-4]"))
                    .filter(e -> e.getEnd().compareTo(annual.getStart()) > 0)
                    .filter(e -> e.getEnd().compareTo(annual.getEnd()) <= 0)
                    .collect(Collectors.toList());

            if (quarters.size() == 4) continue;
            if (quarters.size() != 3) {
                log.warn("Annual {} has {} quarters (expected 3 or 4), skipping fiscal Q4 derivation", frame, quarters.size());
                continue;
            }

            long q4Val = annual.getVal() - quarters.stream().mapToLong(FactEntryDto::getVal).sum();

            LocalDate endDate = LocalDate.parse(annual.getEnd());
            int calendarQuarter = (endDate.getMonthValue() - 1) / 3 + 1;
            String q4Frame = "CY" + endDate.getYear() + "Q" + calendarQuarter;

            FactEntryDto derived = new FactEntryDto();
            derived.setFrame(q4Frame);
            derived.setVal(q4Val);
            derived.setEnd(annual.getEnd());
            derived.setForm("derived");

            byFrame.put(q4Frame, derived);
            log.info("Derived {} = {} (from annual {})", q4Frame, q4Val, frame);
        }
    }

    private String[] parseFrame(String frame) {
        Matcher m = FRAME_PATTERN.matcher(frame);
        if (!m.matches()) {
            log.warn("Unrecognized frame format: {}", frame);
            return null;
        }
        return new String[]{m.group(1), m.group(2) != null ? m.group(2) : "FY"};
    }

    private long countDerived(Map<String, FactEntryDto> byFrame) {
        return byFrame.values().stream().filter(e -> "derived".equals(e.getForm())).count();
    }

    private String fetchCompanyFacts(int cik) {
        String url = String.format("https://data.sec.gov/api/xbrl/companyfacts/CIK%010d.json", cik);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "KeystoneAnalytics adampoper@gmail.com");
        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        } catch (Exception e) {
            log.error("Failed to fetch company facts for CIK {}", cik, e);
            return null;
        }
    }
}