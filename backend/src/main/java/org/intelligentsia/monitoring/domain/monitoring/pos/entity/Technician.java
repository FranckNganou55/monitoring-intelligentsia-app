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

@Entity @Table(name = "technicians") @EntityListeners(AuditingEntityListener.class) @Getter @Setter @NoArgsConstructor
public class Technician {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "institution_id", nullable = false) private Institution institution;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 50) private String phone;
    @Column(columnDefinition = "TEXT") private String cards;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @CreatedBy @Column(name = "created_by") private Long createdBy;
    @LastModifiedBy @Column(name = "updated_by") private Long updatedBy;
}
