package org.intelligentsia.monitoring.domain.monitoring.alert.repository;

import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MonitoringAlertRepository
        extends JpaRepository<MonitoringAlert, Long> {

    Page<MonitoringAlert> findAllByOrderByDetectedAtDesc(
            Pageable pageable
    );

    List<MonitoringAlert> findByStatusOrderByDetectedAtDesc(
            MonitoringAlert.AlertStatus status
    );

    List<MonitoringAlert> findByRuleIdAndStatusIn(
            Long ruleId,
            List<MonitoringAlert.AlertStatus> statuses
    );

    long countByRuleId(Long ruleId);

    long countByRuleIdAndDetectedAtAfter(
            Long ruleId,
            LocalDateTime date
    );

    long countByStatus(
            MonitoringAlert.AlertStatus status
    );

    long countByRiskLevelAndStatusIn(
            MonitoringRule.RiskLevel riskLevel,
            List<MonitoringAlert.AlertStatus> statuses
    );

    @Query("""
        SELECT a FROM MonitoringAlert a
        WHERE a.deletedAt IS NULL
          AND a.status <> :archivedStatus
          AND a.detectedAt < :cutoff
          AND (:afterId IS NULL OR a.id > :afterId)
        ORDER BY a.id
        """)
    List<MonitoringAlert> findExpiredForAutoArchive(
            @Param("archivedStatus") MonitoringAlert.AlertStatus archivedStatus,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(a) FROM MonitoringAlert a
        WHERE a.category = :category AND a.deletedAt IS NULL
          AND (:institutionId IS NULL OR a.institutionId = :institutionId)
          AND a.detectedAt >= :from AND a.detectedAt < :until
        """)
    long countCategoryBetween(
            @Param("category") MonitoringAlert.Category category,
            @Param("institutionId") Long institutionId,
            @Param("from") LocalDateTime from,
            @Param("until") LocalDateTime until
    );

    @Query("""
        SELECT COUNT(a) FROM MonitoringAlert a
        WHERE a.category = :category AND a.deletedAt IS NULL
          AND (:institutionId IS NULL OR a.institutionId = :institutionId)
        """)
    long countCategory(
            @Param("category") MonitoringAlert.Category category,
            @Param("institutionId") Long institutionId
    );

    @Query(value = """
        SELECT date_trunc('hour', a.detected_at) AS hour_bucket, COUNT(*) AS total
        FROM monitoring_alerts a
        WHERE a.category = 'FRAUD' AND a.deleted_at IS NULL
          AND a.detected_at >= :since
          AND (:institutionId IS NULL OR a.institution_id = :institutionId)
        GROUP BY 1 ORDER BY 1
        """, nativeQuery = true)
    List<Object[]> countFraudByHourSince(
            @Param("institutionId") Long institutionId,
            @Param("since") LocalDateTime since
    );

    @Query("""
        SELECT a
        FROM MonitoringAlert a
        LEFT JOIN a.rule r
        LEFT JOIN a.atm atm
        LEFT JOIN atm.institution atmInstitution
        WHERE a.deletedAt IS NULL
          AND (:includeArchived = true OR a.status <> :archivedStatus)
          AND (:status IS NULL OR a.status = :status)
          AND (:category IS NULL OR a.category = :category)
          AND (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
          AND (:ruleId IS NULL OR r.id = :ruleId)
          AND (:institutionId IS NULL OR a.institutionId = :institutionId
               OR atmInstitution.id = :institutionId)
          AND (:atmId IS NULL OR a.atm.id = :atmId)
          AND (CAST(:detectedFrom AS timestamp) IS NULL OR a.detectedAt >= :detectedFrom)
          AND (CAST(:detectedUntil AS timestamp) IS NULL OR a.detectedAt < :detectedUntil)
          AND (:year IS NULL OR EXTRACT(YEAR FROM a.detectedAt) = :year)
          AND (:month IS NULL OR EXTRACT(MONTH FROM a.detectedAt) = :month)
          AND (:day IS NULL OR EXTRACT(DAY FROM a.detectedAt) = :day)
          AND (:hour IS NULL OR EXTRACT(HOUR FROM a.detectedAt) = :hour)
          AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(a.transactionReference, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY a.detectedAt DESC, a.id DESC
        """)
    Page<MonitoringAlert> findWithFilters(
            @Param("status") MonitoringAlert.AlertStatus status,
            @Param("includeArchived") boolean includeArchived,
            @Param("archivedStatus") MonitoringAlert.AlertStatus archivedStatus,
            @Param("category") MonitoringAlert.Category category,
            @Param("riskLevel") MonitoringRule.RiskLevel riskLevel,
            @Param("ruleId") Long ruleId,
            @Param("institutionId") Long institutionId,
            @Param("atmId") Long atmId,
            @Param("search") String search,
            @Param("detectedFrom") LocalDateTime detectedFrom,
            @Param("detectedUntil") LocalDateTime detectedUntil,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("day") Integer day,
            @Param("hour") Integer hour,
            Pageable pageable
    );
}
