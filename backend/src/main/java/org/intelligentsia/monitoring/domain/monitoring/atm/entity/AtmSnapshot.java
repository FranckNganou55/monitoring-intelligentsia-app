package org.intelligentsia.monitoring.domain.monitoring.atm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Currency;
import java.time.Instant;

@Entity @Table(name = "atm_snapshots") @Getter @Setter @NoArgsConstructor
public class AtmSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "atm_id", nullable = false) private Atm atm;
    @Column(name = "snapshot_time", nullable = false) private Instant snapshotTime;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "currency1_id") private Currency currency1;
    @Column private Integer note1; @Column private Integer quantity1;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "currency2_id") private Currency currency2;
    @Column private Integer note2; @Column private Integer quantity2;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "currency3_id") private Currency currency3;
    @Column private Integer note3; @Column private Integer quantity3;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "currency4_id") private Currency currency4;
    @Column private Integer note4; @Column private Integer quantity4;
    @Column(name = "status_id", length = 10) private String statusId;
    @Column(name = "status_desc", length = 200) private String statusDesc;
    @Column(name = "all_ok") private Boolean allOk;
    @Column(name = "status_message") private String statusMessage;
    @Column(name = "threshold1_exceeded", nullable = false) private boolean threshold1Exceeded = false;
    @Column(name = "threshold2_exceeded", nullable = false) private boolean threshold2Exceeded = false;
}
