package com.adampoperdevelopment.keystoneanalytics.repository;

import com.adampoperdevelopment.keystoneanalytics.entity.Company;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRowEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialStatementRowEntryRepository extends JpaRepository<FinancialStatementRowEntry, Integer> {

    boolean existsByCompanyAndFinancialStatementRow(Company company, FinancialStatementRow row);
}