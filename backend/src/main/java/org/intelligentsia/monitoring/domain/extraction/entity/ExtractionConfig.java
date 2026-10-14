package org.intelligentsia.monitoring.domain.extraction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "extraction_configs")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class ExtractionConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ExtractionTemplate template;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** JSON object {"paramName": "value"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "param_values", columnDefinition = "jsonb")
    private String paramValues;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 10)
    private OutputFormat outputFormat;

    /** Cron expression — null means manual-only */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractionDestinataire> destinataires = new ArrayList<>();

    @CreatedDate  @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @CreatedBy  @Column(name = "created_by") private Long createdBy;
    @LastModifiedBy @Column(name = "updated_by") private Long updatedBy;
}
