package com.adampoperdevelopment.keystoneanalytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XbrlTagDto {

    private String label;
    private String description;
    private Map<String, List<FactEntryDto>> units;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, List<FactEntryDto>> getUnits() { return units; }
    public void setUnits(Map<String, List<FactEntryDto>> units) { this.units = units; }
}