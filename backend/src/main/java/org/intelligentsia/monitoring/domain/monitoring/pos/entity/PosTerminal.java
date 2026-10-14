package org.intelligentsia.monitoring.domain.monitoring.pos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity @Table(name = "pos_terminals") @EntityListeners(AuditingEntityListener.class) @Getter @Setter @NoArgsConstructor
public class PosTerminal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "institution_id", nullable = false) private Institution institution;
    @Column(name = "terminal_id", nullable = false, unique = true, length = 50) private String terminalId;
    @Column(length = 200) private String merchant;
    @Column(length = 100) private String city;
    @Column(length = 100) private String district;
    @Column(name = "extra_name", length = 200) private String extraName;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "technician_id") private Technician technician;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @CreatedBy @Column(name = "created_by") private Long createdBy;
    @LastModifiedBy @Column(name = "updated_by") private Long updatedBy;
}
