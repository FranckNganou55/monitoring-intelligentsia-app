package org.intelligentsia.monitoring.domain.monitoring.pos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "pos_telecollect_snapshots") @Getter @Setter @NoArgsConstructor
public class PosTelecollectSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "institution_id", nullable = false) private Institution institution;
    @Column(name = "terminal_id", nullable = false, length = 50) private String terminalId;
    @Column(name = "collect_date", nullable = false) private LocalDate collectDate;
    @Column(length = 50) private String status;
    @Column(name = "snapshot_time", nullable = false) private Instant snapshotTime;
}
