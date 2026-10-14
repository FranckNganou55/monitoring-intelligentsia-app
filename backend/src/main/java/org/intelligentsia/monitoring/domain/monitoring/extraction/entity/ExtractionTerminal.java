package org.intelligentsia.monitoring.domain.monitoring.extraction.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "extraction_terminals") @Getter @Setter @NoArgsConstructor
public class ExtractionTerminal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "batch_id", nullable = false) private ExtractionBatch batch;
    @Column(name = "terminal_id", nullable = false, length = 50) private String terminalId;
}
