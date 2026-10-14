package org.intelligentsia.monitoring.domain.notification.inapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.settings.service.MailConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEmailNotificationService {

    private final MailConfigService mailConfig;

    @Value("${app.notifications.alert-email.enabled:true}")
    private boolean enabled;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(MonitoringAlertEmailRequested request) {
        if (!enabled || !mailConfig.isAlertMailEnabled()
                || request.recipient() == null || request.recipient().isBlank()) return;
        try {
            JavaMailSender sender = mailConfig.buildSender();
            var message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailConfig.getFromAddress(), mailConfig.getFromName());
            helper.setTo(request.recipient());
            helper.setSubject(request.subject());
            helper.setText(request.body(), false);
            sender.send(message);
            log.info("E-mail d’alerte envoyé à {}", request.recipient());
        } catch (Exception ex) {
            log.warn("Notification e-mail d’alerte impossible à envoyer à {} : {}",
                    request.recipient(), ex.getMessage());
        }
    }
}
