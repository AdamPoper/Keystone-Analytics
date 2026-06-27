package com.adampoperdevelopment.keystoneanalytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EdgarCompanyTickerDto {

    @JsonProperty("cik_str")
    private Integer cikStr;

    @JsonProperty("ticker")
    private String ticker;

    @JsonProperty("title")
    private String title;

    public Integer getCikStr() { return cikStr; }
    public String getTicker() { return ticker; }
    public String getTitle() { return title; }
}