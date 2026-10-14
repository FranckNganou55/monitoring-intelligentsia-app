package org.intelligentsia.monitoring.domain.extraction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "extraction_destinataires")
@Getter @Setter @NoArgsConstructor
public class ExtractionDestinataire {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private ExtractionConfig config;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 100)
    private String nom;

    @Column(nullable = false)
    private boolean active = true;
}
