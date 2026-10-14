package org.intelligentsia.monitoring.domain.monitoring.network.repository;

import org.intelligentsia.monitoring.domain.monitoring.network.entity.NetworkSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface NetworkSnapshotRepository extends JpaRepository<NetworkSnapshot, Long> {
    @Query("SELECT s FROM NetworkSnapshot s WHERE s.id = (SELECT MAX(s2.id) FROM NetworkSnapshot s2 WHERE s2.networkCode = s.networkCode AND s2.snapshotTime = (SELECT MAX(s3.snapshotTime) FROM NetworkSnapshot s3 WHERE s3.networkCode = s.networkCode))")
    List<NetworkSnapshot> findAllLatest();

    @Query("SELECT s FROM NetworkSnapshot s ORDER BY s.snapshotTime DESC")
    Page<NetworkSnapshot> findHistory(Pageable pageable);

    void deleteBySnapshotTimeBefore(Instant cutoff);
}
