package org.intelligentsia.monitoring.domain.notification.inapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column
    private String message;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Alerte de monitoring à l'origine de la notification.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private MonitoringAlert alert;

    /**
     * Niveau de risque associé à la notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 30)
    private RiskLevel riskLevel;

    /**
     * Indique si la notification doit déclencher un son.
     */
    @Column(name = "sound_required", nullable = false)
    private boolean soundRequired = false;

    /**
     * Indique si la notification doit déclencher
     * un signal visuel urgent.
     */
    @Column(name = "visual_alert_required", nullable = false)
    private boolean visualAlertRequired = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum RiskLevel {
        TYPIQUE,
        MOINS_CRITIQUE,
        MOYEN,
        CRITIQUE
    }
}
