package com.adampoperdevelopment.keystoneanalytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyFactsDto {

    @JsonProperty("us-gaap")
    private Map<String, XbrlTagDto> usGaap;

    public Map<String, XbrlTagDto> getUsGaap() { return usGaap; }
    public void setUsGaap(Map<String, XbrlTagDto> usGaap) { this.usGaap = usGaap; }
}