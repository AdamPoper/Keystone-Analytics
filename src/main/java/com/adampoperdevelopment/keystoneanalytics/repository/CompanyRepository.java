package com.adampoperdevelopment.keystoneanalytics.repository;

import com.adampoperdevelopment.keystoneanalytics.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Company findByCik(Integer cik);
}