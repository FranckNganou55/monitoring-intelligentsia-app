package org.intelligentsia.monitoring.domain.monitoring.fraud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;

import java.time.Instant;

@Entity
@Table(name = "fraud_rates")
@Getter @Setter
public class FraudRate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(nullable = false, length = 10)
    private String bin;

    @Column(nullable = false)
    private Integer suspectCount = 0;

    @Column(nullable = false)
    private Integer fraudCount = 0;

    @Column(nullable = false)
    private Instant snapshotTime;
}
