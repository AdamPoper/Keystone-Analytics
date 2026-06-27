package com.adampoperdevelopment.keystoneanalytics.service;

import com.adampoperdevelopment.keystoneanalytics.dto.EdgarCompanyTickerDto;
import com.adampoperdevelopment.keystoneanalytics.entity.Company;
import com.adampoperdevelopment.keystoneanalytics.entity.Ticker;
import com.adampoperdevelopment.keystoneanalytics.repository.CompanyRepository;
import com.adampoperdevelopment.keystoneanalytics.repository.TickerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EdgarSyncService {

    private static final Logger log = LoggerFactory.getLogger(EdgarSyncService.class);
    private static final String COMPANY_TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";

    private final CompanyRepository companyRepository;
    private final TickerRepository tickerRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EdgarSyncService(CompanyRepository companyRepository,
                            TickerRepository tickerRepository,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.companyRepository = companyRepository;
        this.tickerRepository = tickerRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncCompaniesAndTickers() {
        log.info("Starting EDGAR company/ticker sync...");

        Map<String, EdgarCompanyTickerDto> entries = fetchAndParse();
        if (entries == null) return;

        // Load all existing DB records up front for efficient lookup
        Map<Integer, Company> existingByCik = companyRepository.findAll()
                .stream().collect(Collectors.toMap(Company::getCik, c -> c));

        // If duplicate ticker symbols exist in DB, keep the active one
        Map<String, Ticker> existingBySymbol = tickerRepository.findAll()
                .stream().collect(Collectors.toMap(
                        Ticker::getTickerSymbol,
                        t -> t,
                        (a, b) -> Boolean.TRUE.equals(a.getIsActive()) ? a : b));

        // Track what the EDGAR response contains
        Set<Integer> edgarCiks = new HashSet<>();
        Set<String> edgarSymbols = new HashSet<>();

        // Use maps keyed by DB id to deduplicate pending saves
        Map<Integer, Company> companiesToSave = new LinkedHashMap<>();
        Map<Integer, Ticker> tickersToSave = new LinkedHashMap<>();

        // Local cache so multiple tickers for the same CIK share one Company instance
        Map<Integer, Company> companyCache = new HashMap<>();

        for (EdgarCompanyTickerDto dto : entries.values()) {
            edgarCiks.add(dto.getCikStr());
            edgarSymbols.add(dto.getTicker());

            Company company = resolveCompany(
                    dto, existingByCik, companyCache, companiesToSave);

            resolveTicker(dto, company, existingBySymbol, tickersToSave);
        }

        // Deactivate companies absent from the EDGAR response
        for (Company company : existingByCik.values()) {
            if (!edgarCiks.contains(company.getCik()) && !Boolean.FALSE.equals(company.getIsActive())) {
                company.setIsActive(false);
                companiesToSave.put(company.getId(), company);
                log.info("Deactivated company: '{}' (CIK: {})", company.getName(), company.getCik());
            }
        }

        // Deactivate tickers absent from the EDGAR response
        for (Ticker ticker : existingBySymbol.values()) {
            if (!edgarSymbols.contains(ticker.getTickerSymbol()) && Boolean.TRUE.equals(ticker.getIsActive())) {
                ticker.setIsActive(false);
                tickersToSave.put(ticker.getId(), ticker);
                log.info("Deactivated ticker: '{}'", ticker.getTickerSymbol());
            }
        }

        companyRepository.saveAll(companiesToSave.values());
        tickerRepository.saveAll(tickersToSave.values());

        log.info("EDGAR sync complete: {} company saves, {} ticker saves.",
                companiesToSave.size(), tickersToSave.size());
    }

    private Company resolveCompany(EdgarCompanyTickerDto dto,
                                   Map<Integer, Company> existingByCik,
                                   Map<Integer, Company> companyCache,
                                   Map<Integer, Company> companiesToSave) {
        if (companyCache.containsKey(dto.getCikStr())) {
            return companyCache.get(dto.getCikStr());
        }

        Company company;

        if (existingByCik.containsKey(dto.getCikStr())) {
            company = existingByCik.get(dto.getCikStr());
            boolean dirty = false;

            if (!dto.getTitle().equals(company.getName())) {
                log.info("Updated company name: '{}' -> '{}' (CIK: {})",
                        company.getName(), dto.getTitle(), company.getCik());
                company.setName(dto.getTitle());
                dirty = true;
            }
            if (!Boolean.TRUE.equals(company.getIsActive())) {
                log.info("Reactivated company: '{}' (CIK: {})", company.getName(), company.getCik());
                company.setIsActive(true);
                dirty = true;
            }
            if (dirty) companiesToSave.put(company.getId(), company);

        } else {
            company = new Company();
            company.setCik(dto.getCikStr());
            company.setName(dto.getTitle());
            company.setIsActive(true);
            company.setCreatedAt(System.currentTimeMillis());
            company = companyRepository.save(company);
            log.info("Created company: '{}' (CIK: {})", company.getName(), company.getCik());
        }

        companyCache.put(dto.getCikStr(), company);
        return company;
    }

    private void resolveTicker(EdgarCompanyTickerDto dto,
                                Company company,
                                Map<String, Ticker> existingBySymbol,
                                Map<Integer, Ticker> tickersToSave) {
        if (existingBySymbol.containsKey(dto.getTicker())) {
            Ticker ticker = existingBySymbol.get(dto.getTicker());
            if (!Boolean.TRUE.equals(ticker.getIsActive())) {
                ticker.setIsActive(true);
                tickersToSave.put(ticker.getId(), ticker);
                log.info("Reactivated ticker: '{}' for company '{}'",
                        dto.getTicker(), company.getName());
            }
        } else {
            Ticker ticker = new Ticker();
            ticker.setCompany(company);
            ticker.setTickerSymbol(dto.getTicker());
            ticker.setIsActive(true);
            ticker.setCreatedAt(System.currentTimeMillis());
            tickersToSave.put(System.identityHashCode(ticker), ticker);
            log.info("Created ticker: '{}' for company '{}'", dto.getTicker(), company.getName());
        }
    }

    private Map<String, EdgarCompanyTickerDto> fetchAndParse() {
        String json = fetchEdgarTickers();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse EDGAR response", e);
            return null;
        }
    }

    private String fetchEdgarTickers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "KeystoneAnalytics contact@keystoneanalytics.com");
        return restTemplate.exchange(
                COMPANY_TICKERS_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();
    }
}