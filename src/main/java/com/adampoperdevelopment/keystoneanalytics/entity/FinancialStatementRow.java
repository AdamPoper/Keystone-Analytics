package com.adampoperdevelopment.keystoneanalytics.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "financial_statement_row")
public class FinancialStatementRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "title", nullable = false, length = 45)
    private String title;

    @Column(name = "description", length = 90)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_category", nullable = false, length = 45)
    private StatementCategory statementCategory;

    @Column(name = "is_calculated", columnDefinition = "TINYINT(1)")
    private Boolean isCalculated;

    @Column(name = "created_at")
    private Long createdAt;

    @OneToMany(mappedBy = "financialStatementRow")
    private List<FinancialStatementRowEntry> financialStatementRowEntrys = new ArrayList<>();

    @OneToMany(mappedBy = "financialStatementRow")
    private List<FinancialStatementRowTag> financialStatementRowTags = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public StatementCategory getStatementCategory() {
        return statementCategory;
    }

    public void setStatementCategory(StatementCategory statementCategory) {
        this.statementCategory = statementCategory;
    }

    public Boolean getIsCalculated() {
        return isCalculated;
    }

    public void setIsCalculated(Boolean isCalculated) {
        this.isCalculated = isCalculated;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public List<FinancialStatementRowEntry> getFinancialStatementRowEntrys() {
        return financialStatementRowEntrys;
    }

    public void setFinancialStatementRowEntrys(List<FinancialStatementRowEntry> financialStatementRowEntrys) {
        this.financialStatementRowEntrys = financialStatementRowEntrys;
    }

    public List<FinancialStatementRowTag> getFinancialStatementRowTags() {
        return financialStatementRowTags;
    }

    public void setFinancialStatementRowTags(List<FinancialStatementRowTag> financialStatementRowTags) {
        this.financialStatementRowTags = financialStatementRowTags;
    }

}
