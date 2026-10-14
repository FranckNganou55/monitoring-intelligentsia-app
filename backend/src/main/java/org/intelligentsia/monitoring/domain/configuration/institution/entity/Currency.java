package org.intelligentsia.monitoring.domain.configuration.institution.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "currencies") @Getter @Setter @NoArgsConstructor
public class Currency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "iso_numeric", nullable = false, unique = true, length = 3) private String isoNumeric;
    @Column(name = "iso_alpha", nullable = false, unique = true, length = 3) private String isoAlpha;
    @Column(nullable = false, length = 100) private String label;
}
