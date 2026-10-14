package org.intelligentsia.monitoring.domain.configuration.settings.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailConfigServiceTest {
    @Test
    void v10SettingsOverrideEnvironmentAndPreservePassword() {
        var settings = mock(AppSettingService.class);
        when(settings.getString("mail.smtp.host")).thenReturn("smtp.database.test");
        when(settings.getString("mail.smtp.port")).thenReturn("2525");
        when(settings.getString("mail.smtp.username")).thenReturn("database-user");
        when(settings.getString("mail.smtp.password")).thenReturn(" secret with spaces ");
        when(settings.getString("mail.from.address")).thenReturn("alerts@database.test");
        when(settings.getString("mail.from.name")).thenReturn("Alertes monétiques");
        when(settings.getString("mail.enabled")).thenReturn("true");
        when(settings.getBoolean("notif.email.enabled", true)).thenReturn(true);
        var environment = new MockEnvironment().withProperty("MAIL_HOST", "localhost")
                .withProperty("MAIL_PORT", "1025").withProperty("MAIL_PASS", "ignored")
                .withProperty("MAIL_FROM", "ignored@environment.test").withProperty("MAIL_ENABLED", "false");
        var configuration = new MailConfigService(settings, environment);
        var sender = (JavaMailSenderImpl) configuration.buildSender();
        assertEquals("smtp.database.test", sender.getHost());
        assertEquals(2525, sender.getPort());
        assertEquals("database-user", sender.getUsername());
        assertEquals(" secret with spaces ", sender.getPassword());
        assertEquals("alerts@database.test", configuration.getFromAddress());
        assertEquals("Alertes monétiques", configuration.getFromName());
        assertTrue(configuration.isAlertMailEnabled());
    }

    @Test
    void disablingMailInDatabaseCannotBeOverriddenByEnvironment() {
        var settings = mock(AppSettingService.class);
        when(settings.getString("mail.enabled")).thenReturn("false");
        var configuration = new MailConfigService(settings, new MockEnvironment().withProperty("MAIL_ENABLED", "true"));
        assertFalse(configuration.isMailEnabled());
        assertFalse(configuration.isAlertMailEnabled());
    }

    @ParameterizedTest
    @ValueSource(ints = {465, 587, 2525, 1025})
    void usesTheCorrectTransportForDatabasePort(int port) {
        var settings = mock(AppSettingService.class);
        when(settings.getString("mail.smtp.port")).thenReturn("" + port);
        when(settings.getString("mail.smtp.ssl")).thenReturn("false");
        var sender = (JavaMailSenderImpl) new MailConfigService(settings, new MockEnvironment()).buildSender();
        var properties = sender.getJavaMailProperties();
        assertEquals(port, sender.getPort());
        assertEquals(port == 465, Boolean.parseBoolean(properties.getProperty("mail.smtp.ssl.enable")));
        assertEquals(port == 587 || port == 2525, Boolean.parseBoolean(properties.getProperty("mail.smtp.starttls.enable")));
    }
}
