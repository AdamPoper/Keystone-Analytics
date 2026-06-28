package com.adampoperdevelopment.keystoneanalytics.dto;

public class FinancialEntryDto {

    private String fiscalYear;
    private String fiscalPeriod;
    private String reportDate;
    private Long value;

    public FinancialEntryDto(String fiscalYear, String fiscalPeriod, String reportDate, Long value) {
        this.fiscalYear = fiscalYear;
        this.fiscalPeriod = fiscalPeriod;
        this.reportDate = reportDate;
        this.value = value;
    }

    public String getFiscalYear() { return fiscalYear; }
    public String getFiscalPeriod() { return fiscalPeriod; }
    public String getReportDate() { return reportDate; }
    public Long getValue() { return value; }
}