package org.intelligentsia.monitoring.domain.monitoring.host.repository;

import org.intelligentsia.monitoring.domain.monitoring.host.entity.HostSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface HostSnapshotRepository extends JpaRepository<HostSnapshot, Long> {
    @Query("SELECT s FROM HostSnapshot s WHERE s.snapshotTime = (SELECT MAX(s2.snapshotTime) FROM HostSnapshot s2)")
    List<HostSnapshot> findAllLatest();

    @Query("SELECT s FROM HostSnapshot s ORDER BY s.snapshotTime DESC")
    Page<HostSnapshot> findHistory(Pageable pageable);

    void deleteBySnapshotTimeBefore(Instant cutoff);
}
