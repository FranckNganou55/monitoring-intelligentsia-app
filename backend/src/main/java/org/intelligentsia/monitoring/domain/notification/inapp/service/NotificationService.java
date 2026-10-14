package org.intelligentsia.monitoring.domain.notification.inapp.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.repository.UserRepository;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.notification.inapp.dto.NotificationDto;
import org.intelligentsia.monitoring.domain.notification.inapp.entity.Notification;
import org.intelligentsia.monitoring.domain.notification.inapp.repository.NotificationRepository;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.Scheduled;

import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.auth.entity.UserStatus;

import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final SseEmitterService sseService;
    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbc;

    /**
     * Méthode historique de création d'une notification.
     *
     * Elle est conservée afin de ne pas casser les fonctionnalités
     * existantes du projet.
     */
    @Transactional
    public void send(
            Long userId,
            String type,
            String title,
            String message,
            String entityType,
            Long entityId
    ) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return;
        }

        Notification notification = new Notification();

        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);

        repo.save(notification);

        sseService.send(userId, toDto(notification));
    }

    /**
     * Création d'une notification liée à une alerte de monitoring.
     *
     * Les propriétés soundRequired et visualAlertRequired sont
     * déterminées par le niveau de risque de l'alerte.
     */
    @Transactional
    public void sendAlertNotification(
            Long userId,
            MonitoringAlert alert,
            String title,
            String message
    ) {
        if (userId == null || alert == null) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        if (alert.getRule() == null
            || !Boolean.TRUE.equals(alert.getRule().getNotificationEnabled())) {
                return;
            }

        Notification notification = new Notification();

        notification.setUser(user);
        notification.setType("ALERT");
        notification.setTitle(title);
        notification.setMessage(message);

        notification.setEntityType("MONITORING_ALERT");
        notification.setEntityId(alert.getId());

        notification.setAlert(alert);

        Notification.RiskLevel riskLevel = mapRiskLevel(alert);

        notification.setRiskLevel(riskLevel);

        notification.setSoundRequired(true);
        notification.setVisualAlertRequired(true);

        repo.save(notification);

        sseService.send(userId, toDto(notification));

    }

    @Transactional
    public void sendAlertNotificationToRecipients(
            MonitoringAlert alert,
            String title,
            String message
    ) {
        if (alert == null || alert.getRule() == null) {
            return;
        }

        if (!Boolean.TRUE.equals(alert.getRule().getNotificationEnabled())) {
            return;
        }

        List<User> users = userRepository.findAllByStatus(UserStatus.ACTIVE);

        Long alertInstitutionId = alert.getInstitutionId();

        for (User user : users) {

            /*
            * L'utilisateur doit disposer de la permission
            * de consultation des alertes.
            */
            boolean canViewAlerts = user.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            "MONITORING_ALERT_VIEW"
                                    .equals(authority.getAuthority())
                    );

            if (!canViewAlerts) {
                continue;
            }

            /*
            * ADMIN et GLOBAL :
            * peuvent recevoir les alertes de toutes les institutions.
            */
            if (user.getScope() == UserScope.ADMIN
                    || user.getScope() == UserScope.GLOBAL) {

                sendAlertNotification(
                        user.getId(),
                        alert,
                        title,
                        message
                );

                continue;
            }

            /*
            * INSTITUTION :
            * uniquement les alertes de son institution.
            */
            if (user.getScope() == UserScope.INSTITUTION) {

                if (alertInstitutionId == null
                        || user.getInstitution() == null
                        || !alertInstitutionId.equals(
                                user.getInstitution().getId()
                        )) {
                    continue;
                }

                sendAlertNotification(
                        user.getId(),
                        alert,
                        title,
                        message
                );
            }
        }
    }

    public boolean sendConfiguredFraudAlertEmail(MonitoringAlert alert, Long expectedInstitutionId) {
        if (alert.getCategory() != MonitoringAlert.Category.FRAUD) return false;
        String title = alert.getTitle();
        String message = alert.getDescription();
        Map<String, Object> fraud = fraudDetails(alert.getFraudDetectionId());
        // Match the institution the operator selected against every supported
        // association. Older alerts can have a stale denormalized institution_id
        // while still being correctly linked through their ATM or fraud source.
        Long fraudInstitutionId = fraud.get("institution_id") instanceof Number id ? id.longValue() : null;
        Long atmInstitutionId = alert.getAtm() != null && alert.getAtm().getInstitution() != null
                ? alert.getAtm().getInstitution().getId() : null;
        boolean belongsToSelectedInstitution = expectedInstitutionId != null
                && (expectedInstitutionId.equals(alert.getInstitutionId())
                    || expectedInstitutionId.equals(atmInstitutionId)
                    || expectedInstitutionId.equals(fraudInstitutionId));
        if (!belongsToSelectedInstitution) return false;
        Long institutionId = expectedInstitutionId;

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        if (institution == null) return false;
        String recipient = switch (alert.getRiskLevel() == null ? "" : alert.getRiskLevel().name()) {
            case "TYPIQUE" -> institution.getAlertEmailTypique();
            case "MOINS_CRITIQUE" -> institution.getAlertEmailMoinsCritique();
            case "MOYEN" -> institution.getAlertEmailMoyen();
            case "CRITIQUE" -> institution.getAlertEmailCritique();
            default -> null;
        };
        if (recipient == null || recipient.isBlank()) return false;

        String risk = alert.getRiskLevel() == null ? "NON DÉFINI" : alert.getRiskLevel().name();
        String subject = "[Intelligentsia Monitoring] " + risk + " — " + title;
        String institutionDetails = "Institution : " + institution.getName() + " (" + institution.getCode() + ")";
        eventPublisher.publishEvent(new MonitoringAlertEmailRequested(
                recipient.trim(), subject, buildAlertEmailDetails(alert, title, message, institutionDetails)));
        return true;
    }

    private String buildAlertEmailDetails(MonitoringAlert alert, String title, String message, String institutionDetails) {
        String ruleCode = alert.getRule() == null ? "—" : alert.getRule().getCode();
        String ruleName = alert.getRule() == null ? "—" : alert.getRule().getName();
        String atmDetails = "—";
        if (alert.getAtm() != null) {
            var atm = alert.getAtm();
            atmDetails = atm.getTerminalId()
                    + (atm.getAddress() == null ? "" : " | " + atm.getAddress())
                    + (atm.getCity() == null ? "" : " | " + atm.getCity());
        }
        Map<String, Object> fraud = fraudDetails(alert.getFraudDetectionId());
        String cardNumber = maskCard((String) fraud.get("account_reference"));
        Object amount = fraud.get("amount");
        String amountText = amount == null ? "—" : amount + (fraud.get("currency") == null ? "" : " " + fraud.get("currency"));
        String transactionReference = alert.getTransactionReference();
        if ((transactionReference == null || transactionReference.isBlank()) && fraud.get("retrieval_reference") != null) {
            transactionReference = fraud.get("retrieval_reference").toString();
        }
        return """
                Une nouvelle alerte de monitoring a été détectée.

                Identifiant : %s
                Titre : %s
                Niveau de risque : %s
                Catégorie : %s
                Statut : %s
                Détectée le : %s
                Règle : %s — %s
                Carte : %s
                Montant : %s
                Institution : %s
                GAB : %s
                Référence transaction : %s
                Identifiant fraude : %s
                Type de transaction : %s
                Commerçant : %s
                Code pays transaction : %s

                Détails :
                %s
                """.formatted(alert.getId(), title, alert.getRiskLevel(), alert.getCategory(), alert.getStatus(),
                alert.getDetectedAt(), ruleCode, ruleName, cardNumber, amountText,
                institutionDetails == null || institutionDetails.isBlank()
                        ? (alert.getInstitutionId() == null ? "—" : alert.getInstitutionId()) : institutionDetails,
                atmDetails, transactionReference == null ? "—" : transactionReference,
                alert.getFraudDetectionId() == null ? "—" : alert.getFraudDetectionId(),
                valueOrDash(fraud.get("transaction_type")), valueOrDash(fraud.get("merchant_name")),
                valueOrDash(fraud.get("country_code")),
                message == null || message.isBlank() ? "—" : message);
    }

    private Map<String, Object> fraudDetails(Long fraudDetectionId) {
        if (fraudDetectionId == null) return java.util.Collections.emptyMap();
        try {
            return jdbc.queryForMap("""
                    SELECT account_reference, amount, currency, retrieval_reference,
                           transaction_type, merchant_name, country_code, institution_id
                    FROM fraud_detections WHERE id = ?
                    """, fraudDetectionId);
        } catch (EmptyResultDataAccessException ignored) {
            return java.util.Collections.emptyMap();
        }
    }

    private String valueOrDash(Object value) {
        return value == null || value.toString().isBlank() ? "—" : value.toString();
    }

    private String maskCard(String source) {
        if (source == null) return "—";
        String digits = source.replaceAll("\\D", "");
        if (digits.length() < 8 || digits.length() > 19) return "—";
        return digits.substring(0, 4) + "*".repeat(digits.length() - 8) + digits.substring(digits.length() - 4);
    }

    /**
     * Récupération paginée des notifications d'un utilisateur.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> findByUser(
            Long userId,
            int page,
            int size,
            Long institutionId
    ) {
        return PageResponse.of(
                repo.findVisibleByUserAndInstitution(
                        userId,
                        institutionId,
                        Instant.now().minus(Duration.ofDays(1)),
                        PageRequest.of(page, size)
                ).map(this::toDto)
        );
    }

    /**
     * Nombre de notifications non lues.
     */
    @Transactional(readOnly = true)
    public long countUnread(Long userId, Long institutionId) {
        return repo.countUnreadByUserAndInstitution(userId, institutionId);
    }

    @Transactional(readOnly = true)
    public List<Long> findOpenAlertIds(Long userId, Long institutionId) {
        return repo.findAlertIdsByUserAndStatus(userId,
                org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert.AlertStatus.OPEN,
                institutionId);
    }

    /**
     * Marque toutes les notifications de l'utilisateur
     * comme lues.
     */
    @Transactional
    public void markAllRead(Long userId, Long institutionId) {
        repo.markAllReadByUserAndInstitution(userId, institutionId, Instant.now());
    }

    @Transactional
    public void markRead(Long notificationId, Long userId, Long institutionId) {
        if (repo.markReadByIdAndUser(notificationId, userId, institutionId, Instant.now()) == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Notification introuvable.");
        }
    }

    @Scheduled(cron = "0 20 2 * * *")
    @Transactional
    public void deleteExpiredReadNotifications() {
        repo.deleteReadBefore(Instant.now().minus(Duration.ofDays(1)));
    }

    /**
     * Conversion Entity -> DTO.
     */
    private NotificationDto toDto(Notification notification) {

        NotificationDto dto = new NotificationDto();

        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());

        dto.setRead(notification.isRead());

        dto.setEntityType(notification.getEntityType());
        dto.setEntityId(notification.getEntityId());

        if (notification.getAlert() != null) {
            var alert = notification.getAlert();
            dto.setAlertId(alert.getId());
            dto.setCategory(alert.getCategory() == null ? null : alert.getCategory().name());
            Long institutionId = alert.getInstitutionId();
            if (institutionId == null && alert.getAtm() != null && alert.getAtm().getInstitution() != null) {
                institutionId = alert.getAtm().getInstitution().getId();
            }
            if (institutionId == null && alert.getFraudDetectionId() != null) {
                Object sourceInstitutionId = fraudDetails(alert.getFraudDetectionId()).get("institution_id");
                if (sourceInstitutionId instanceof Number numericId) institutionId = numericId.longValue();
            }
            dto.setInstitutionId(institutionId);
            dto.setTransactionReference(alert.getTransactionReference());
            dto.setDetectedAt(alert.getDetectedAt());
            if (alert.getRule() != null) {
                dto.setRuleCode(alert.getRule().getCode());
                dto.setRuleName(alert.getRule().getName());
            }
            if (alert.getAtm() != null) {
                dto.setTerminalId(alert.getAtm().getTerminalId());
            }
        }

        if (notification.getRiskLevel() != null) {
            dto.setRiskLevel(notification.getRiskLevel().name());
        }

        dto.setSoundRequired(notification.isSoundRequired());
        dto.setVisualAlertRequired(notification.isVisualAlertRequired());

        dto.setCreatedAt(notification.getCreatedAt());

        return dto;
    }

    /**
     * Conversion du niveau de risque de l'alerte vers
     * le niveau utilisé par le système de notifications.
     *
     * Cette méthode est isolée pour permettre de l'adapter
     * facilement à l'enum exact de MonitoringAlert.
     */
    private Notification.RiskLevel mapRiskLevel(MonitoringAlert alert) {

        if (alert.getRiskLevel() == null) {
            return Notification.RiskLevel.TYPIQUE;
        }

        return switch (alert.getRiskLevel().name()) {
            case "CRITIQUE" ->
                    Notification.RiskLevel.CRITIQUE;

            case "MOYEN" ->
                    Notification.RiskLevel.MOYEN;

            case "MOINS_CRITIQUE" ->
                    Notification.RiskLevel.MOINS_CRITIQUE;

            case "TYPIQUE" ->
                    Notification.RiskLevel.TYPIQUE;

            default ->
                    Notification.RiskLevel.TYPIQUE;
        };
    }
}
