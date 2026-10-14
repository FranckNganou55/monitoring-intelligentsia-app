package org.intelligentsia.monitoring.domain.monitoring.transaction.repository;

import org.intelligentsia.monitoring.domain.monitoring.transaction.entity.TransactionRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface TransactionRateRepository extends JpaRepository<TransactionRate, Long> {
    @Query("SELECT s FROM TransactionRate s WHERE s.snapshotTime = (SELECT MAX(s2.snapshotTime) FROM TransactionRate s2)")
    List<TransactionRate> findAllLatest();

    @Query("SELECT s FROM TransactionRate s ORDER BY s.snapshotTime DESC")
    Page<TransactionRate> findHistory(Pageable pageable);

    @Query("SELECT s FROM TransactionRate s WHERE (CAST(:from AS timestamp) IS NULL OR s.snapshotTime >= :from) AND (CAST(:to AS timestamp) IS NULL OR s.snapshotTime <= :to) ORDER BY s.snapshotTime DESC")
    Page<TransactionRate> findHistoryBetween(@org.springframework.data.repository.query.Param("from") Instant from,
                                             @org.springframework.data.repository.query.Param("to")   Instant to,
                                             Pageable pageable);

    void deleteBySnapshotTimeBefore(Instant cutoff);

    @Query("""
        SELECT s FROM TransactionRate s
        WHERE (CAST(:from AS timestamp) IS NULL OR s.snapshotTime >= :from) AND (CAST(:to AS timestamp) IS NULL OR s.snapshotTime <= :to)
          AND (CAST(:network AS string) IS NULL OR s.network = :network)
          AND (CAST(:state AS string) IS NULL OR EXISTS (
            SELECT n.id FROM NetworkSnapshot n
            WHERE (n.networkCode = s.network OR n.description = s.network)
              AND n.snapshotTime = (SELECT MAX(n2.snapshotTime) FROM NetworkSnapshot n2 WHERE n2.networkCode = n.networkCode)
              AND UPPER(COALESCE(n.operatingStatus, 'UNKNOWN')) = :state
          ))
        ORDER BY s.snapshotTime DESC, s.id DESC
        """)
    Page<TransactionRate> findFiltered(Instant from, Instant to, String network, String state, Pageable pageable);
}
