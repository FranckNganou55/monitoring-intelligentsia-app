package org.intelligentsia.monitoring.domain.monitoring.fleet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Currency;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cards_over_limit")
@Getter @Setter
public class CardOverLimit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(name = "card_number", nullable = false, length = 30)
    private String cardNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "limit_amount")   private BigDecimal limitAmount;
    @Column(name = "current_amount") private BigDecimal currentAmount;

    @Column(nullable = false)
    private Boolean exceeded = false;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime;
}
