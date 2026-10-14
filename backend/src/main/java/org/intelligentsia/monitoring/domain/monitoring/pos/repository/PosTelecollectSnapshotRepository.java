package org.intelligentsia.monitoring.domain.monitoring.pos.repository;

import org.intelligentsia.monitoring.domain.monitoring.pos.entity.PosTelecollectSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface PosTelecollectSnapshotRepository extends JpaRepository<PosTelecollectSnapshot, Long> {
    @Query("SELECT s FROM PosTelecollectSnapshot s WHERE s.institution.id = :institutionId ORDER BY s.snapshotTime DESC")
    Page<PosTelecollectSnapshot> findByInstitutionId(Long institutionId, Pageable pageable);

    @Query("""
        SELECT s FROM PosTelecollectSnapshot s
        WHERE s.snapshotTime = (
            SELECT MAX(s2.snapshotTime) FROM PosTelecollectSnapshot s2
            WHERE s2.terminalId = s.terminalId AND s2.institution.id = s.institution.id
        )
          AND (:institutionId IS NULL OR s.institution.id = :institutionId)
        """)
    List<PosTelecollectSnapshot> findLatestByInstitution(@org.springframework.data.repository.query.Param("institutionId") Long institutionId);

    List<PosTelecollectSnapshot> findByInstitutionIdAndCollectDate(Long institutionId, LocalDate collectDate);

    void deleteBySnapshotTimeBefore(Instant cutoff);
}
