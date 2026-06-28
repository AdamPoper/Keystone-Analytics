package com.adampoperdevelopment.keystoneanalytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyFactsResponseDto {

    private int cik;
    private String entityName;
    private CompanyFactsDto facts;

    public int getCik() { return cik; }
    public void setCik(int cik) { this.cik = cik; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public CompanyFactsDto getFacts() { return facts; }
    public void setFacts(CompanyFactsDto facts) { this.facts = facts; }
}