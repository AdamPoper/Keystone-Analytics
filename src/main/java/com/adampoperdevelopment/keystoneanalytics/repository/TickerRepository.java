package com.adampoperdevelopment.keystoneanalytics.repository;

import com.adampoperdevelopment.keystoneanalytics.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TickerRepository extends JpaRepository<Ticker, Integer> {
}