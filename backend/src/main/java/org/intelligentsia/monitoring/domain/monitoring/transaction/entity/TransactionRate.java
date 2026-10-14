package org.intelligentsia.monitoring.domain.monitoring.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "transaction_rates") @Getter @Setter @NoArgsConstructor
public class TransactionRate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 100) private String network;
    @Column(length = 100) private String acquirer;
    @Column(length = 100) private String issuer;
    @Column(name = "success_count") private Integer successCount;
    @Column(name = "success_rate", precision = 6, scale = 2) private BigDecimal successRate;
    @Column(name = "failure_count") private Integer failureCount;
    @Column(name = "failure_rate", precision = 6, scale = 2) private BigDecimal failureRate;
    @Column(name = "snapshot_time", nullable = false) private Instant snapshotTime;
}
