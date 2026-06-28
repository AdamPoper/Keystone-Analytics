package com.adampoperdevelopment.keystoneanalytics.dto;

import java.util.List;

public class FinancialTermDto {

    private String name;
    private String category;
    private List<FinancialEntryDto> entries;

    public FinancialTermDto(String name, String category, List<FinancialEntryDto> entries) {
        this.name = name;
        this.category = category;
        this.entries = entries;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public List<FinancialEntryDto> getEntries() { return entries; }
}