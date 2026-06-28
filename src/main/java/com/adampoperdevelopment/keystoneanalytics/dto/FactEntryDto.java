package com.adampoperdevelopment.keystoneanalytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FactEntryDto {

    private String start;
    private String end;
    private Long val;
    private String accn;
    private Integer fy;
    private String fp;
    private String form;
    private String filed;
    private String frame;

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public Long getVal() { return val; }
    public void setVal(Long val) { this.val = val; }

    public String getAccn() { return accn; }
    public void setAccn(String accn) { this.accn = accn; }

    public Integer getFy() { return fy; }
    public void setFy(Integer fy) { this.fy = fy; }

    public String getFp() { return fp; }
    public void setFp(String fp) { this.fp = fp; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public String getFiled() { return filed; }
    public void setFiled(String filed) { this.filed = filed; }

    public String getFrame() { return frame; }
    public void setFrame(String frame) { this.frame = frame; }
}