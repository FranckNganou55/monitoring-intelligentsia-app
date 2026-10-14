package org.intelligentsia.monitoring.domain.monitoring.fraud.repository;

import org.intelligentsia.monitoring.domain.monitoring.fraud.entity.FraudDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FraudDetailRepository extends JpaRepository<FraudDetail, Long> {

    @Query("""
        SELECT f FROM FraudDetail f
        WHERE (:institutionId IS NULL OR f.institution.id = :institutionId)
          AND (:search IS NULL OR f.bin LIKE %:search% OR f.ruleCode LIKE %:search%
               OR f.ruleDesc LIKE %:search% OR f.description LIKE %:search%)
        ORDER BY f.snapshotTime DESC
        """)
    Page<FraudDetail> findWithFilters(
            @Param("search") String search,
            @Param("institutionId") Long institutionId,
            Pageable pageable);

    @Query("SELECT COUNT(f) FROM FraudDetail f WHERE (:institutionId IS NULL OR f.institution.id = :institutionId) AND f.snapshotTime > :since")
    long countRecent(
        @Param("institutionId") Long institutionId, 
        @Param("since") Instant since);

    @Query("""
        SELECT COUNT(f)
        FROM FraudDetail f
        WHERE (:institutionId IS NULL OR f.institution.id = :institutionId)
        """)
    long countTotal(@Param("institutionId") Long institutionId);

    @Query("""
        SELECT f
        FROM FraudDetail f
        WHERE f.snapshotTime >= :since
        AND (:institutionId IS NULL OR f.institution.id = :institutionId)
        ORDER BY f.snapshotTime ASC
        """)
    List<FraudDetail> findHistory(
        @Param("institutionId") Long institutionId,
        @Param("since") Instant since);
}
