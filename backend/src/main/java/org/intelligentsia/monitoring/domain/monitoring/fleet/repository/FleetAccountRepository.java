package org.intelligentsia.monitoring.domain.monitoring.fleet.repository;

import org.intelligentsia.monitoring.domain.monitoring.fleet.entity.FleetAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FleetAccountRepository extends JpaRepository<FleetAccount, Long> {

    @Query("""
        SELECT f FROM FleetAccount f
        WHERE (:institutionId IS NULL OR f.institution.id = :institutionId)
          AND (:search IS NULL OR f.accountNumber LIKE %:search%)
        ORDER BY f.snapshotTime DESC
        """)
    Page<FleetAccount> findWithFilters(
            @Param("search") String search,
            @Param("institutionId") Long institutionId,
            Pageable pageable);

    /** Returns the most recent balance for a given institution + currency, used to compute fleet deltas. */
    Optional<FleetAccount> findFirstByInstitutionIdAndCurrencyIdOrderBySnapshotTimeDesc(
            Long institutionId, Long currencyId);
}
