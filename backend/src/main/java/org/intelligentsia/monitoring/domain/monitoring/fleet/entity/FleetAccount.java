package org.intelligentsia.monitoring.domain.monitoring.fleet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Currency;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fleet_accounts")
@Getter @Setter
public class FleetAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    private BigDecimal balance;
    @Column(name = "previous_balance") private BigDecimal previousBalance;
    private BigDecimal delta1;
    private BigDecimal delta2;

    @Column(name = "delta1_exceeded", nullable = false)
    private Boolean delta1Exceeded = false;

    @Column(name = "delta2_exceeded", nullable = false)
    private Boolean delta2Exceeded = false;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime;
}
