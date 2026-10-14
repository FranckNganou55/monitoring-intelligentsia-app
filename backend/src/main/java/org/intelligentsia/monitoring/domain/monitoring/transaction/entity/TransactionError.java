package org.intelligentsia.monitoring.domain.monitoring.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "transaction_errors") @Getter @Setter @NoArgsConstructor
public class TransactionError {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 100) private String network;
    @Column(name = "response_code", length = 10) private String responseCode;
    @Column(name = "response_desc", length = 200) private String responseDesc;
    @Column private Integer count;
    @Column(name = "snapshot_time", nullable = false) private Instant snapshotTime;
}
