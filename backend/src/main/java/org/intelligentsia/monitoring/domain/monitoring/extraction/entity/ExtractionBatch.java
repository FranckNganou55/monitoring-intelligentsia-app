package org.intelligentsia.monitoring.domain.monitoring.extraction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "extraction_batches") @Getter @Setter @NoArgsConstructor
public class ExtractionBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "institution_id", nullable = false) private Institution institution;
    @Column(nullable = false, length = 10) private String type;
    @Column(name = "start_date") private Instant startDate;
    @Column(name = "end_date") private Instant endDate;
    @Column(length = 200) private String merchant;
    @Column(length = 100) private String city;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "created_by") private Long createdBy;
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractionTerminal> terminals = new ArrayList<>();
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractionTransaction> transactions = new ArrayList<>();
}
