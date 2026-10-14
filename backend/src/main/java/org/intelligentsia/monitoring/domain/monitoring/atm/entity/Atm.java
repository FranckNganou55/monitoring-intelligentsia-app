package org.intelligentsia.monitoring.domain.monitoring.atm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Currency;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "atms") @EntityListeners(AuditingEntityListener.class) @Getter @Setter @NoArgsConstructor
public class Atm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "institution_id", nullable = false) private Institution institution;
    @Column(name = "terminal_id", nullable = false, length = 50) private String terminalId;
    private String address;
    @Column(length = 100) private String city;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "currency1_id") private Currency currency1;
    @Column(name = "threshold1", precision = 20, scale = 2) private BigDecimal threshold1;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "currency2_id") private Currency currency2;
    @Column(name = "threshold2", precision = 20, scale = 2) private BigDecimal threshold2;
    @Column(name = "svfe_pid") private Long svfePid;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @CreatedBy @Column(name = "created_by") private Long createdBy;
    @LastModifiedBy @Column(name = "updated_by") private Long updatedBy;
}
