package org.intelligentsia.monitoring.domain.monitoring.rule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "monitoring_rules")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MonitoringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_order")
    private Integer sourceOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RiskLevel riskLevel = RiskLevel.MOYEN;

    @Column(name = "risk_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    @Column(name = "sound_enabled", nullable = false)
    private Boolean soundEnabled = false;

    @Column(name = "visual_alert_enabled", nullable = false)
    private Boolean visualAlertEnabled = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RuleCategory {
        FRAUD,
        ATM,
        NETWORK
    }

    public enum RiskLevel {
        TYPIQUE,
        MOINS_CRITIQUE,
        MOYEN,
        CRITIQUE
    }
}
