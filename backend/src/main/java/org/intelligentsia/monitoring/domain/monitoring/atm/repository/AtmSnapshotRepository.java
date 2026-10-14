package org.intelligentsia.monitoring.domain.monitoring.atm.repository;

import org.intelligentsia.monitoring.domain.monitoring.atm.entity.AtmSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AtmSnapshotRepository extends JpaRepository<AtmSnapshot, Long> {
    Optional<AtmSnapshot> findFirstByAtm_IdOrderBySnapshotTimeDesc(Long atmId);

    @Query("SELECT s FROM AtmSnapshot s JOIN s.atm a WHERE a.institution.id = :institutionId ORDER BY s.snapshotTime DESC LIMIT 1")
    List<AtmSnapshot> findLatestByInstitutionId(Long institutionId);

    @Query("""
        SELECT s FROM AtmSnapshot s JOIN FETCH s.atm a
        WHERE s.snapshotTime = (
            SELECT MAX(s2.snapshotTime) FROM AtmSnapshot s2 WHERE s2.atm.id = a.id
        )
          AND (:institutionId IS NULL OR a.institution.id = :institutionId)
        """)
    List<AtmSnapshot> findLatestByInstitution(@Param("institutionId") Long institutionId);

    @Query("SELECT s FROM AtmSnapshot s WHERE s.atm.id = :atmId ORDER BY s.snapshotTime DESC")
    Page<AtmSnapshot> findByAtmIdOrderByTimeDesc(Long atmId, Pageable pageable);

    @Query("""
        SELECT s
        FROM AtmSnapshot s
        WHERE s.snapshotTime >= :since
        AND (:institutionId IS NULL OR s.atm.institution.id = :institutionId)
        ORDER BY s.snapshotTime ASC
        """)
    List<AtmSnapshot> findHistory(
        @Param("institutionId") Long institutionId,
        @Param("since") Instant since);

    void deleteBySnapshotTimeBefore(Instant cutoff);
}
