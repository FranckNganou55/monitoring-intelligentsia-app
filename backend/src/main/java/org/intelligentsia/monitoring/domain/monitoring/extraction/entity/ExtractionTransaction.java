package org.intelligentsia.monitoring.domain.monitoring.extraction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "extraction_transactions") @Getter @Setter @NoArgsConstructor
public class ExtractionTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "batch_id", nullable = false) private ExtractionBatch batch;
    @Column(name = "terminal_id", length = 50) private String terminalId;
    @Column(name = "transaction_date") private Instant transactionDate;
    @Column(precision = 20, scale = 2) private BigDecimal amount;
    @Column(name = "response_code", length = 10) private String responseCode;
    @Column(length = 50) private String status;
    @Column(length = 200) private String merchant;
    @Column(length = 100) private String city;
}
