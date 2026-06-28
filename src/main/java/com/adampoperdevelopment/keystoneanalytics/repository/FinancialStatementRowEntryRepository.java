package com.adampoperdevelopment.keystoneanalytics.repository;

import com.adampoperdevelopment.keystoneanalytics.entity.Company;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRowEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FinancialStatementRowEntryRepository extends JpaRepository<FinancialStatementRowEntry, Integer> {

    boolean existsByCompanyAndFinancialStatementRow(Company company, FinancialStatementRow row);

    @Query("SELECT e FROM FinancialStatementRowEntry e " +
           "WHERE e.company = :company " +
           "AND e.financialStatementRow = :row " +
           "AND e.reportDate >= :from " +
           "AND e.reportDate <= :to " +
           "AND e.fiscalPeriod IN :periods " +
           "ORDER BY e.reportDate ASC")
    List<FinancialStatementRowEntry> findByFilters(
            @Param("company") Company company,
            @Param("row") FinancialStatementRow row,
            @Param("from") String from,
            @Param("to") String to,
            @Param("periods") List<String> periods);
}