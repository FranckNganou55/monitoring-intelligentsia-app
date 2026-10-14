package org.intelligentsia.monitoring.domain.configuration.settings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailConfigService {

    private final AppSettingService settings;
    private final Environment environment;

    /**
     * Builds a JavaMailSender from current application settings (a local file in standalone mode).
     * AppSettingService is Caffeine-cached, so this is cheap to call.
     */
    public JavaMailSender buildSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // V10 app_settings (Administration > Paramètres > Mail) is authoritative.
        // Environment values only support installations where a setting is absent.
        String host = setting("MAIL_HOST", "mail.smtp.host", "localhost");
        int port = integerSetting("MAIL_PORT", "mail.smtp.port", 587);
        String user = setting("MAIL_USER", "mail.smtp.username", "");
        String pass = setting("MAIL_PASS", "mail.smtp.password", "");
        boolean ssl = booleanSetting("MAIL_SSL", "mail.smtp.ssl", false);

        // Port-based override: 465 uses direct SSL/TLS; 587 uses STARTTLS.
        // Local SMTP capture tools (for example MailHog on 1025) use plain SMTP.
        if (port == 465) ssl = true;
        if (port == 587) ssl = false;
        boolean startTls = port == 587 || (port != 25 && port != 1025 && !ssl);

        sender.setHost(host != null ? host : "localhost");
        sender.setPort(port);
        sender.setUsername(user);
        sender.setPassword(pass);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(user != null && !user.isBlank()));

        if (ssl) {
            // Port 465 — SSL/TLS direct connection
            props.put("mail.smtp.ssl.enable",    "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        } else if (startTls) {
            // STARTTLS SMTP (for example provider port 587 or Mailtrap 2525).
            // Do NOT set mail.smtp.ssl.enable or mail.smtp.ssl.protocols here —
            // those trigger a direct SSL handshake which breaks on a STARTTLS port.
            props.put("mail.smtp.starttls.enable",    "true");
            props.put("mail.smtp.starttls.required",  "true");
            props.put("mail.smtp.starttls.protocols", "TLSv1.2 TLSv1.3");
        } else {
            // Plain SMTP (local MailHog on 1025, or explicitly configured port 25).
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
        }

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout",           "10000");
        props.put("mail.smtp.writetimeout",      "10000");

        log.debug("MailSender configuré: {}:{} ssl={}", host, port, ssl);
        return sender;
    }

    public String getFromAddress() {
        String v = setting("MAIL_FROM", "mail.from.address", "");
        return v != null && !v.isBlank() ? v : "noreply@monitoring.local";
    }

    public String getFromName() {
        String v = setting("MAIL_FROM_NAME", "mail.from.name", "");
        return v != null && !v.isBlank() ? v : "Monitoring";
    }

    public boolean isMailEnabled() {
        return booleanSetting("MAIL_ENABLED", "mail.enabled", false);
    }

    public boolean isAlertMailEnabled() {
        return isMailEnabled() && settings.getBoolean("notif.email.enabled", true);
    }

    private String setting(String environmentKey, String settingKey, String fallback) {
        String value = settings.getString(settingKey);
        if (value == null) value = environment.getProperty(environmentKey);
        if (value == null) return fallback;
        // Authentication passwords must be preserved exactly, including spaces.
        return "mail.smtp.password".equals(settingKey) ? value : value.trim();
    }

    private int integerSetting(String environmentKey, String settingKey, int fallback) {
        String value = setting(environmentKey, settingKey, String.valueOf(fallback));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Valeur SMTP invalide pour {} ; le port par défaut sera utilisé.", settingKey);
            return fallback;
        }
    }

    private boolean booleanSetting(String environmentKey, String settingKey, boolean fallback) {
        String value = setting(environmentKey, settingKey, String.valueOf(fallback));
        return Boolean.parseBoolean(value.trim());
    }
}
