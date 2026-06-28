package com.adampoperdevelopment.keystoneanalytics.repository;

import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import com.adampoperdevelopment.keystoneanalytics.entity.StatementCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialStatementRowRepository extends JpaRepository<FinancialStatementRow, Integer> {

    FinancialStatementRow findByTitle(String title);

    FinancialStatementRow findByTitleIgnoreCase(String title);

    List<FinancialStatementRow> findByStatementCategory(StatementCategory category);
}