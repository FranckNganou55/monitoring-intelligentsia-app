package org.intelligentsia.monitoring.domain.notification.inapp.service;

public record MonitoringAlertEmailRequested(String recipient, String subject, String body) {
}
