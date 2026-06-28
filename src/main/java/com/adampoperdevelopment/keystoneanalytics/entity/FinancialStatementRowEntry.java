package com.adampoperdevelopment.keystoneanalytics.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "financial_statement_row_entry")
public class FinancialStatementRowEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "financial_statement_row_id", nullable = false, insertable = false, updatable = false)
    private Integer financialStatementRowId;

    @Column(name = "value", nullable = false)
    private Long value;

    @Column(name = "company_id", nullable = false, insertable = false, updatable = false)
    private Integer companyId;

    @Column(name = "report_date", nullable = false, length = 45)
    private String reportDate;

    @Column(name = "fiscal_year", length = 45)
    private String fiscalYear;

    @Column(name = "fiscal_period", length = 45)
    private String fiscalPeriod;

    @Column(name = "created_at")
    private Long createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_statement_row_id", nullable = false)
    private FinancialStatementRow financialStatementRow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFinancialStatementRowId() {
        return financialStatementRowId;
    }

    public void setFinancialStatementRowId(Integer financialStatementRowId) {
        this.financialStatementRowId = financialStatementRowId;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public FinancialStatementRow getFinancialStatementRow() {
        return financialStatementRow;
    }

    public void setFinancialStatementRow(FinancialStatementRow financialStatementRow) {
        this.financialStatementRow = financialStatementRow;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

}
