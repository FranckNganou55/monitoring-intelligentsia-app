package org.intelligentsia.monitoring.domain.monitoring.host.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "host_snapshots") @Getter @Setter @NoArgsConstructor
public class HostSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 200) private String hostname;
    @Column(length = 200) private String description;
    @Column(name = "ip_address", length = 50) private String ipAddress;
    @Column(name = "operating_status", length = 50) private String operatingStatus;
    @Column(name = "processing_status", length = 50) private String processingStatus;
    @Column(name = "snapshot_time", nullable = false) private Instant snapshotTime;
}
