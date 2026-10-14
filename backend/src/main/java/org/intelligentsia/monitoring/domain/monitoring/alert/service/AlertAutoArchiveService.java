package org.intelligentsia.monitoring.domain.monitoring.alert.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.AlertEvent;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.alert.repository.AlertEventRepository;
import org.intelligentsia.monitoring.domain.monitoring.alert.repository.MonitoringAlertRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertAutoArchiveService {
    private static final int BATCH_SIZE = 250;

    private final MonitoringAlertRepository alerts;
    private final AlertEventRepository events;

    @Transactional
    public int archiveExpired(int ageInDays) {
        int days = Math.max(1, ageInDays);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(days);
        Long afterId = null;
        int archivedCount = 0;

        while (true) {
            List<MonitoringAlert> batch = alerts.findExpiredForAutoArchive(
                    MonitoringAlert.AlertStatus.ARCHIVED,
                    cutoff,
                    afterId,
                    PageRequest.of(0, BATCH_SIZE)
            );
            if (batch.isEmpty()) break;

            for (MonitoringAlert alert : batch) {
                String oldStatus = alert.getStatus().name();
                alert.setStatus(MonitoringAlert.AlertStatus.ARCHIVED);
                alert.setArchivedAt(now);
                alerts.save(alert);

                AlertEvent event = new AlertEvent();
                event.setAlert(alert);
                event.setEventType(AlertEvent.EventType.ARCHIVED);
                event.setOldStatus(oldStatus);
                event.setNewStatus(MonitoringAlert.AlertStatus.ARCHIVED.name());
                event.setComment("Archivage automatique après " + days + " jour(s).");
                events.save(event);
                archivedCount++;
            }
            afterId = batch.get(batch.size() - 1).getId();
        }
        return archivedCount;
    }
}
