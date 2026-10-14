package org.intelligentsia.monitoring.domain.notification.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.settings.service.MailConfigService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailConfigService mailConfig;
    private final TemplateEngine   templateEngine;

    @Async
    public void sendWelcome(String to, String firstName) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        send(to, "Bienvenue sur Monitoring Monétique", "email/welcome", ctx);
    }

    @Async
    public void sendPasswordReset(String to, String firstName, String token) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("token", token);
        send(to, "Réinitialisation de votre mot de passe", "email/reset-password", ctx);
    }

    @Async
    public void sendAtmAlert(String to, String terminalId, String message) {
        Context ctx = new Context();
        ctx.setVariable("terminalId", terminalId);
        ctx.setVariable("message", message);
        send(to, "Alerte ATM — " + terminalId, "email/alert-atm", ctx);
    }

    @Async
    public void sendFraudAlert(String to, String bin, String institution, int fraudCount) {
        Context ctx = new Context();
        ctx.setVariable("bin", bin);
        ctx.setVariable("institution", institution);
        ctx.setVariable("fraudCount", fraudCount);
        send(to, "Alerte Fraude — BIN " + bin, "email/alert-fraud", ctx);
    }

    private void send(String to, String subject, String template, Context ctx) {
        if (!mailConfig.isMailEnabled()) {
            log.debug("Emails désactivés — message '{}' ignoré pour {}", subject, to);
            return;
        }
        try {
            JavaMailSender sender = mailConfig.buildSender();
            String html = templateEngine.process(template, ctx);
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(mailConfig.getFromAddress(), mailConfig.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(msg);
            log.debug("Email '{}' envoyé à {}", subject, to);
        } catch (Exception e) {
            log.error("Erreur envoi email à {}: {}", to, e.getMessage());
        }
    }
}
