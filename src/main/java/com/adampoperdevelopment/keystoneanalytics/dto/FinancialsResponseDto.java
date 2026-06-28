package com.adampoperdevelopment.keystoneanalytics.dto;

import java.util.List;

public class FinancialsResponseDto {

    private String ticker;
    private String company;
    private String period;
    private String from;
    private String to;
    private List<FinancialTermDto> terms;

    public FinancialsResponseDto(String ticker, String company,
                                  String period, String from, String to,
                                  List<FinancialTermDto> terms) {
        this.ticker = ticker;
        this.company = company;
        this.period = period;
        this.from = from;
        this.to = to;
        this.terms = terms;
    }

    public String getTicker() { return ticker; }
    public String getCompany() { return company; }
    public String getPeriod() { return period; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public List<FinancialTermDto> getTerms() { return terms; }
}