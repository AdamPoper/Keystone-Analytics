package com.adampoperdevelopment.keystoneanalytics.repository;

import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialStatementRowRepository extends JpaRepository<FinancialStatementRow, Integer> {

    FinancialStatementRow findByTitle(String title);
}