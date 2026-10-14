package org.intelligentsia.monitoring.domain.monitoring.fleet.repository;

import org.intelligentsia.monitoring.domain.monitoring.fleet.entity.CardOverLimit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardOverLimitRepository extends JpaRepository<CardOverLimit, Long> {

    @Query("""
        SELECT c FROM CardOverLimit c
        WHERE (:institutionId IS NULL OR c.institution.id = :institutionId)
          AND (:search IS NULL OR c.cardNumber LIKE %:search%)
        ORDER BY c.snapshotTime DESC
        """)
    Page<CardOverLimit> findWithFilters(
            @Param("search") String search,
            @Param("institutionId") Long institutionId,
            Pageable pageable);
}
