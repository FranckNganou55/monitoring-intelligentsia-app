package org.intelligentsia.monitoring.domain.notification.inapp.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class NotificationDto {

    private Long id;

    private String type;

    private String title;

    private String message;

    private boolean read;

    private String entityType;

    private Long entityId;

    /**
     * Identifiant de l'alerte associée.
     */
    private Long alertId;

    private String category;
    private Long institutionId;
    private String ruleCode;
    private String ruleName;
    private String transactionReference;
    private String terminalId;
    private java.time.LocalDateTime detectedAt;

    /**
     * Niveau de risque.
     */
    private String riskLevel;

    /**
     * Le frontend doit-il jouer un son ?
     */
    private boolean soundRequired;

    /**
     * Le frontend doit-il afficher le signal visuel urgent ?
     */
    private boolean visualAlertRequired;

    private Instant createdAt;
}
