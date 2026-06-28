package com.adampoperdevelopment.keystoneanalytics.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "financial_statement_row_tag")
public class FinancialStatementRowTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "financial_statement_row_id", nullable = false, insertable = false, updatable = false)
    private Integer financialStatementRowId;

    @Column(name = "tag", nullable = false, length = 128)
    private String tag;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "taxonomy", length = 45)
    private String taxonomy;

    @Column(name = "notes", length = 256)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_statement_row_id", nullable = false)
    private FinancialStatementRow financialStatementRow;

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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(String taxonomy) {
        this.taxonomy = taxonomy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public FinancialStatementRow getFinancialStatementRow() {
        return financialStatementRow;
    }

    public void setFinancialStatementRow(FinancialStatementRow financialStatementRow) {
        this.financialStatementRow = financialStatementRow;
    }

}
