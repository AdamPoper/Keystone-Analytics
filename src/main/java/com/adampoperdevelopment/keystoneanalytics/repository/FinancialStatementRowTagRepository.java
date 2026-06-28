package com.adampoperdevelopment.keystoneanalytics.repository;

import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRowTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialStatementRowTagRepository extends JpaRepository<FinancialStatementRowTag, Integer> {

    List<FinancialStatementRowTag> findByFinancialStatementRowOrderByPriorityAsc(FinancialStatementRow row);
}