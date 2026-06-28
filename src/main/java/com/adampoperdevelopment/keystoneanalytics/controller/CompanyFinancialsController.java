package com.adampoperdevelopment.keystoneanalytics.controller;

import com.adampoperdevelopment.keystoneanalytics.dto.FinancialEntryDto;
import com.adampoperdevelopment.keystoneanalytics.dto.FinancialTermDto;
import com.adampoperdevelopment.keystoneanalytics.dto.FinancialsResponseDto;
import com.adampoperdevelopment.keystoneanalytics.entity.Company;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRowEntry;
import com.adampoperdevelopment.keystoneanalytics.entity.StatementCategory;
import com.adampoperdevelopment.keystoneanalytics.entity.Ticker;
import com.adampoperdevelopment.keystoneanalytics.repository.FinancialStatementRowEntryRepository;
import com.adampoperdevelopment.keystoneanalytics.repository.FinancialStatementRowRepository;
import com.adampoperdevelopment.keystoneanalytics.repository.TickerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyFinancialsController {

    private final TickerRepository tickerRepository;
    private final FinancialStatementRowRepository rowRepository;
    private final FinancialStatementRowEntryRepository entryRepository;

    public CompanyFinancialsController(TickerRepository tickerRepository,
                                       FinancialStatementRowRepository rowRepository,
                                       FinancialStatementRowEntryRepository entryRepository) {
        this.tickerRepository = tickerRepository;
        this.rowRepository = rowRepository;
        this.entryRepository = entryRepository;
    }

    @GetMapping("/{ticker}/financials")
    public ResponseEntity<FinancialsResponseDto> getFinancials(
            @PathVariable String ticker,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String period,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String category) {

        Ticker tickerEntity = tickerRepository.findByTickerSymbolIgnoreCase(ticker);
        if (tickerEntity == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> periods = resolvePeriods(period);
        if (periods == null) {
            return ResponseEntity.badRequest().build();
        }

        List<FinancialStatementRow> rows = resolveRows(term, category);
        if (rows == null) {
            return ResponseEntity.badRequest().build();
        }
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Company company = tickerEntity.getCompany();
        List<FinancialTermDto> termDtos = new ArrayList<>();

        for (FinancialStatementRow row : rows) {
            List<FinancialStatementRowEntry> entries = entryRepository.findByFilters(company, row, from, to, periods);
            if (entries.isEmpty()) continue;

            List<FinancialEntryDto> entryDtos = entries.stream()
                    .map(e -> new FinancialEntryDto(e.getFiscalYear(), e.getFiscalPeriod(), e.getReportDate(), e.getValue()))
                    .toList();

            termDtos.add(new FinancialTermDto(row.getTitle(), row.getStatementCategory().name(), entryDtos));
        }

        return ResponseEntity.ok(new FinancialsResponseDto(
                ticker.toUpperCase(),
                company.getName(),
                period,
                from,
                to,
                termDtos
        ));
    }

    // Returns the rows to query based on the provided filters.
    // Returns null if the params are invalid, empty list if nothing matched.
    private List<FinancialStatementRow> resolveRows(String term, String category) {
        if (term != null && category != null) return null; // mutually exclusive

        if (term != null) {
            FinancialStatementRow row = rowRepository.findByTitleIgnoreCase(term);
            return row != null ? List.of(row) : List.of();
        }

        if (category != null) {
            StatementCategory cat;
            try {
                cat = StatementCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
            return rowRepository.findByStatementCategory(cat);
        }

        return rowRepository.findAll();
    }

    private List<String> resolvePeriods(String period) {
        return switch (period.toLowerCase()) {
            case "quarterly" -> List.of("Q1", "Q2", "Q3", "Q4");
            case "annually"  -> List.of("FY");
            default          -> null;
        };
    }
}