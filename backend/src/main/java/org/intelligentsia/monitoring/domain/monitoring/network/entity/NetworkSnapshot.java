package org.intelligentsia.monitoring.domain.monitoring.network.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "network_snapshots") @Getter @Setter @NoArgsConstructor
public class NetworkSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "network_code", length = 50) private String networkCode;
    @Column(length = 200) private String description;
    @Column(name = "operating_status", length = 50) private String operatingStatus;
    @Column(name = "processing_status", length = 50) private String processingStatus;
    @Column(name = "transaction_mode", length = 50) private String transactionMode;
    @Column(name = "snapshot_time", nullable = false) private Instant snapshotTime;
}
