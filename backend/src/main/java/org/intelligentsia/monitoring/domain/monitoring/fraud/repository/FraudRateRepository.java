package org.intelligentsia.monitoring.domain.monitoring.fraud.repository;

import org.intelligentsia.monitoring.domain.monitoring.fraud.entity.FraudRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudRateRepository extends JpaRepository<FraudRate, Long> {

    @Query("""
        SELECT f FROM FraudRate f
        WHERE (:institutionId IS NULL OR f.institution.id = :institutionId)
          AND (:search IS NULL OR f.bin LIKE %:search%)
        ORDER BY f.snapshotTime DESC
        """)
    Page<FraudRate> findWithFilters(
            @Param("search") String search,
            @Param("institutionId") Long institutionId,
            Pageable pageable);
}
