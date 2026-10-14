package org.intelligentsia.monitoring.domain.monitoring.fraud.report.repository;

import org.intelligentsia.monitoring.domain.monitoring.fraud.report.entity.FraudDailyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface FraudDailyReportRepository extends JpaRepository<FraudDailyReport, Long> {
    Page<FraudDailyReport> findAllByOrderByReportDateDesc(Pageable pageable);
    @org.springframework.data.jpa.repository.Query("SELECT r FROM FraudDailyReport r WHERE r.reportDate >= :fromDate AND r.reportDate <= :toDate ORDER BY r.reportDate DESC")
    Page<FraudDailyReport> findInPeriod(@org.springframework.data.repository.query.Param("fromDate") LocalDate fromDate,
                                      @org.springframework.data.repository.query.Param("toDate") LocalDate toDate, Pageable pageable);
    Optional<FraudDailyReport> findByReportDate(LocalDate reportDate);
}
