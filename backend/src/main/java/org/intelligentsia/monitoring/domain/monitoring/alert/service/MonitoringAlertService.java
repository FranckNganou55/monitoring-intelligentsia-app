package org.intelligentsia.monitoring.domain.monitoring.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.repository.UserRepository;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.CreateAlertRequest;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.MonitoringAlertDto;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.UpdateAlertStatusRequest;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.AlertEvent;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.alert.repository.AlertEventRepository;
import org.intelligentsia.monitoring.domain.monitoring.alert.repository.MonitoringAlertRepository;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.domain.monitoring.rule.repository.MonitoringRuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.intelligentsia.monitoring.domain.monitoring.atm.entity.Atm;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmRepository;
import org.intelligentsia.monitoring.domain.configuration.settings.service.AppSettingService;
import org.intelligentsia.monitoring.domain.notification.inapp.service.NotificationService;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.AlertEventDto;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MonitoringAlertService {

    private final MonitoringAlertRepository alertRepository;
    private final AlertEventRepository eventRepository;
    private final MonitoringRuleRepository ruleRepository;
    private final UserRepository userRepository;

    private final AtmRepository atmRepository;
    private final NotificationService notificationService;
    private final AppSettingService appSettingService;
    private final JdbcTemplate jdbc;

    public MonitoringAlertDto create(CreateAlertRequest request) {

        MonitoringRule rule = ruleRepository.findById(request.getRuleId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Règle introuvable."
                        )
                );

        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La règle est désactivée."
            );
        }

        MonitoringAlert alert = new MonitoringAlert();

        alert.setRule(rule);

        /*
        * La catégorie de l'alerte doit correspondre à la règle.
        * On ne fait pas confiance à une catégorie éventuellement
        * différente envoyée dans la requête.
        */
        alert.setCategory(
                switch (rule.getCategory()) {
                    case FRAUD -> MonitoringAlert.Category.FRAUD;
                    case ATM -> MonitoringAlert.Category.ATM;
                    case NETWORK -> MonitoringAlert.Category.NETWORK;
                }
        );

        // Le niveeau de risque vient également de la règle
        alert.setRiskLevel(rule.getRiskLevel());

        alert.setTitle(
                request.getTitle() != null && !request.getTitle().isBlank()
                        ? request.getTitle()
                        : rule.getName()
        );

        alert.setDescription(request.getDescription());
        alert.setInstitutionId(request.getInstitutionId());
        alert.setFraudDetectionId(request.getFraudDetectionId());
        alert.setTransactionReference(request.getTransactionReference());

        /*
        * Association réelle du GAB si un atmId est fourni.
        */
        if (request.getAtmId() != null) {

            Atm atm = atmRepository.findById(request.getAtmId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "GAB introuvable."
                            )
                    );

            alert.setAtm(atm);
        }

        alert.setStatus(MonitoringAlert.AlertStatus.OPEN);
        alert.setDetectedAt(java.time.LocalDateTime.now());

        MonitoringAlert saved = alertRepository.save(alert);

        /*
        * Historique de création :
        * NULL -> OPEN
        */
        // Keep every alert visible immediately, but delay its notification signal
        // (in-app notification, sound and blocking overlay) by risk. CRITIQUE is immediate.
        boolean deferNotification = saved.getRiskLevel() != MonitoringRule.RiskLevel.CRITIQUE;
        long notificationDelayMs = deferNotification ? notificationDelayMs(saved.getRiskLevel()) : 0;
        String deliveryDetails = deferNotification
                ? pendingDeliveryDetails(saved.getDetectedAt().plusNanos(notificationDelayMs * 1_000_000))
                : null;

        AlertEvent createdEvent = createEvent(
            saved,
            AlertEvent.EventType.CREATED,
            null,
            null,
            MonitoringAlert.AlertStatus.OPEN.name(),
            null
        );
        if (deliveryDetails != null) {
            jdbc.update("UPDATE alert_events SET details = ?::jsonb WHERE id = ?", deliveryDetails, createdEvent.getId());
        }

        if (!deferNotification) {
            notificationService.sendAlertNotificationToRecipients(
                    saved,
                    saved.getTitle(),
                    saved.getDescription()
            );
        }

        /*
        * Notification de la nouvelle alerte.
        *
        * La sélection des destinataires sera branchée selon la
        * stratégie métier des utilisateurs.
        */
        return toDto(saved);
    }

    /**
     * Non-critical notifications are released on their configured risk delay.
     * The alert record itself is created immediately; CRITIQUE notifications
     * bypass this queue and are sent synchronously from create().
     */
    @Scheduled(fixedDelay = 5_000)
    public void dispatchDueAlertNotifications() {
        // Retire stale pending entries left by an acknowledgement that happened before
        // this worker cycle (or before this cancellation rule was deployed).
        jdbc.update("""
                UPDATE alert_events event
                SET details = jsonb_set(event.details, '{cadenceDelivery}', '"CANCELLED"'::jsonb)
                FROM monitoring_alerts alert
                WHERE alert.id = event.alert_id
                  AND event.event_type = 'CREATED'
                  AND event.details->>'cadenceDelivery' = 'PENDING'
                  AND alert.status <> 'OPEN'
                """);

        List<Long> alertIds = jdbc.queryForList("""
                UPDATE alert_events event
                SET details = jsonb_set(event.details, '{cadenceDelivery}', '"SENDING"'::jsonb)
                WHERE event.id IN (
                    SELECT queued.id
                    FROM alert_events queued
                    JOIN monitoring_alerts alert ON alert.id = queued.alert_id
                    WHERE queued.event_type = 'CREATED'
                      AND queued.details->>'cadenceDelivery' = 'PENDING'
                      AND CAST(queued.details->>'dueAt' AS TIMESTAMP) <= NOW()
                      AND alert.status = 'OPEN'
                      AND alert.risk_level <> 'CRITIQUE'
                    ORDER BY queued.id
                    FOR UPDATE OF queued SKIP LOCKED
                    LIMIT 100
                )
                RETURNING event.alert_id
                """, Long.class);

        for (Long alertId : alertIds) {
            try {
                MonitoringAlert alert = alertRepository.findById(alertId).orElse(null);
                if (alert != null && alert.getStatus() == MonitoringAlert.AlertStatus.OPEN
                        && alert.getRiskLevel() != MonitoringRule.RiskLevel.CRITIQUE) {
                    notificationService.sendAlertNotificationToRecipients(alert, alert.getTitle(), alert.getDescription());
                    setDeliveryState(alertId, "DELIVERED", LocalDateTime.now());
                } else {
                    setDeliveryState(alertId, "CANCELLED", LocalDateTime.now());
                }
            } catch (RuntimeException exception) {
                log.error("Échec d’envoi différé pour l’alerte fraude {} : {}", alertId, exception.getMessage());
                setDeliveryState(alertId, "PENDING", LocalDateTime.now().plusSeconds(30));
            }
        }
    }

    private long notificationDelayMs(MonitoringRule.RiskLevel riskLevel) {
        if (riskLevel == MonitoringRule.RiskLevel.CRITIQUE) return 0;
        String suffix = switch (riskLevel) {
            case TYPIQUE -> "TYPIQUE";
            case MOINS_CRITIQUE -> "MOINS_CRITIQUE";
            case MOYEN -> "MOYEN";
            case CRITIQUE -> throw new IllegalStateException("Le niveau critique est toujours immédiat.");
        };
        return Math.max(5_000, appSettingService.getInteger("alerts.fraud.interval." + suffix, 60_000));
    }

    private String pendingDeliveryDetails(LocalDateTime dueAt) {
        return "{\"cadenceDelivery\":\"PENDING\",\"dueAt\":\"" + dueAt + "\"}";
    }

    private void setDeliveryState(Long alertId, String state, LocalDateTime dueAt) {
        jdbc.update("""
                UPDATE alert_events
                SET details = jsonb_set(jsonb_set(details, '{cadenceDelivery}', to_jsonb(?::text)),
                                        '{dueAt}', to_jsonb(?::text))
                WHERE alert_id = ? AND event_type = 'CREATED'
                  AND details->>'cadenceDelivery' = 'SENDING'
                """, state, dueAt.toString(), alertId);
    }

    public MonitoringAlertDto updateStatus(
            Long alertId,
            UpdateAlertStatusRequest request,
            Long actorUserId
    ) {
        MonitoringAlert alert = findAlert(alertId);

        MonitoringAlert.AlertStatus oldStatus =
                alert.getStatus();

        MonitoringAlert.AlertStatus newStatus =
                request.getStatus();

        if (newStatus == MonitoringAlert.AlertStatus.RESOLVED
                && (request.getComment() == null || request.getComment().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un commentaire est obligatoire avant de résoudre une alerte.");
        }

        if (oldStatus == newStatus) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "L'alerte est déjà dans le statut " + newStatus + "."
            );
        }

        if (!isValidTransition(oldStatus, newStatus)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Transition de statut non autorisée : "
                    + oldStatus + " → " + newStatus
            );
        }

        User actor = null;

        if (actorUserId != null) {
            actor = userRepository.findById(actorUserId)
                    .orElse(null);
        }

        alert.setStatus(newStatus);

        switch (newStatus) {

            case ACKNOWLEDGED ->
                    alert.setAcknowledgedAt(
                            java.time.LocalDateTime.now()
                    );

            case RESOLVED ->
                    alert.setResolvedAt(
                            java.time.LocalDateTime.now()
                    );

            case ARCHIVED ->
                    alert.setArchivedAt(
                            java.time.LocalDateTime.now()
                    );

            default -> {
                // Aucun timestamp supplémentaire.
            }
        }

        MonitoringAlert saved =
                alertRepository.save(alert);

        AlertEvent.EventType eventType =
                eventTypeFor(newStatus);

        createEvent(
                saved,
                eventType,
                actor,
                oldStatus.name(),
                newStatus.name(),
                request.getComment()
        );

        if (newStatus != MonitoringAlert.AlertStatus.OPEN) {
            jdbc.update("""
                    UPDATE alert_events
                    SET details = jsonb_set(details, '{cadenceDelivery}', '"CANCELLED"'::jsonb)
                    WHERE alert_id = ? AND event_type = 'CREATED'
                      AND details->>'cadenceDelivery' = 'PENDING'
                    """, alertId);
        }

        return toDto(saved);
    }

    public void sendConfiguredFraudEmail(Long alertId, Long institutionId) {
        MonitoringAlert alert = findAlert(alertId);
        if (alert.getCategory() != MonitoringAlert.Category.FRAUD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les alertes de fraude peuvent être envoyées par cette action.");
        }
        if (!notificationService.sendConfiguredFraudAlertEmail(alert, institutionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aucune adresse email n’est configurée pour le niveau de risque et l’institution de cette alerte.");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<MonitoringAlertDto> findAll(
            MonitoringAlert.AlertStatus status,
            MonitoringAlert.Category category,
            MonitoringRule.RiskLevel riskLevel,
            Long ruleId,
            Long institutionId,
            Long atmId,
            String search,
            LocalDateTime detectedFrom,
            LocalDateTime detectedUntil,
            Integer year,
            Integer month,
            Integer day,
            Integer hour,
            boolean includeArchived,
            Pageable pageable
    ) {
        return PageResponse.of(
                alertRepository.findWithFilters(
                        status,
                        includeArchived || status == MonitoringAlert.AlertStatus.ARCHIVED,
                        MonitoringAlert.AlertStatus.ARCHIVED,
                        category,
                        riskLevel,
                        ruleId,
                        institutionId,
                        atmId,
                        search,
                        detectedFrom,
                        detectedUntil,
                        year,
                        month,
                        day,
                        hour,
                        pageable
                ).map(this::toDto)
        );
    }

    @Transactional(readOnly = true)
    public List<AlertEventDto> findEvents(Long alertId) {

        // Vérifie également que l'alerte existe.
        findAlert(alertId);

        return eventRepository
                .findByAlertIdOrderByCreatedAtAsc(alertId)
                .stream()
                .map(this::toEventDto)
                .toList();
    }

    private AlertEventDto toEventDto(AlertEvent event) {

        boolean cadenceInternal = event.getDetails() != null && event.getDetails().contains("\"cadenceDelivery\"");
        return AlertEventDto.builder()
                .id(event.getId())
                .alertId(
                        event.getAlert() != null
                                ? event.getAlert().getId()
                                : null
                )
                .eventType(event.getEventType())
                .actorUserId(
                        event.getActorUser() != null
                                ? event.getActorUser().getId()
                                : null
                )
                .oldStatus(event.getOldStatus())
                .newStatus(event.getNewStatus())
                .comment(event.getComment())
                .details(cadenceInternal ? null : event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public MonitoringAlertDto findById(Long id) {
        return toDto(findAlert(id));
    }

    private MonitoringAlert findAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Alerte introuvable."
                        )
                );
    }

    private AlertEvent createEvent(
            MonitoringAlert alert,
            AlertEvent.EventType eventType,
            User actor,
            String oldStatus,
            String newStatus,
            String comment
    ) {
        AlertEvent event = new AlertEvent();

        event.setAlert(alert);
        event.setEventType(eventType);
        event.setActorUser(actor);
        event.setOldStatus(oldStatus);
        event.setNewStatus(newStatus);
        event.setComment(comment);
        return eventRepository.save(event);
    }

    private AlertEvent.EventType eventTypeFor(
            MonitoringAlert.AlertStatus status
    ) {
        return switch (status) {
            case ACKNOWLEDGED ->
                    AlertEvent.EventType.ACKNOWLEDGED;

            case IN_PROGRESS ->
                    AlertEvent.EventType.IN_PROGRESS;

            case RESOLVED ->
                    AlertEvent.EventType.RESOLVED;

            case ARCHIVED ->
                    AlertEvent.EventType.ARCHIVED;

            case OPEN ->
                    AlertEvent.EventType.REOPENED;
        };
    }

    private MonitoringAlertDto toDto(
            MonitoringAlert alert
    ) {
        MonitoringRule rule = alert.getRule();

        return MonitoringAlertDto.builder()
                .id(alert.getId())
                .ruleId(rule != null ? rule.getId() : null)
                .ruleCode(rule != null ? rule.getCode() : "—")
                .ruleName(rule != null ? rule.getName() : "Règle supprimée")
                .category(alert.getCategory())
                .riskLevel(alert.getRiskLevel())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .status(alert.getStatus())
                .institutionId(alert.getInstitutionId() != null
                        ? alert.getInstitutionId()
                        : (alert.getAtm() != null && alert.getAtm().getInstitution() != null
                                ? alert.getAtm().getInstitution().getId()
                                : null))
                .atmId(
                        alert.getAtm() != null
                                ? alert.getAtm().getId()
                                : null
                )
                .fraudDetectionId(
                        alert.getFraudDetectionId()
                )
                .transactionReference(
                        alert.getTransactionReference()
                )
                .detectedAt(alert.getDetectedAt())
                .acknowledgedAt(
                        alert.getAcknowledgedAt()
                )
                .resolvedAt(
                        alert.getResolvedAt()
                )
                .archivedAt(
                        alert.getArchivedAt()
                )
                .assignedTo(
                        alert.getAssignedTo() != null
                                ? alert.getAssignedTo().getId()
                                : null
                )
                .build();
    }

    private boolean isValidTransition(
        MonitoringAlert.AlertStatus from,
        MonitoringAlert.AlertStatus to
    ) {
        return switch (from) {
            case OPEN ->
                to == MonitoringAlert.AlertStatus.ACKNOWLEDGED;

            case ACKNOWLEDGED ->
                to == MonitoringAlert.AlertStatus.IN_PROGRESS;

            case IN_PROGRESS ->
                to == MonitoringAlert.AlertStatus.RESOLVED;

            case RESOLVED ->
                to == MonitoringAlert.AlertStatus.ARCHIVED
                    || to == MonitoringAlert.AlertStatus.OPEN;

            case ARCHIVED ->
                false;
        };
    }
}
