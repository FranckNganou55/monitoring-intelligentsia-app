package org.intelligentsia.monitoring.domain.monitoring.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertAutoArchiveScheduler {
    private final AlertAutoArchiveService autoArchiveService;

    @Value("${app.alerts.auto-archive-days:7}")
    private int ageInDays;

    @Scheduled(
            fixedDelayString = "${app.alerts.auto-archive-scan-ms:60000}",
            initialDelayString = "${app.alerts.auto-archive-initial-delay-ms:30000}"
    )
    public void archiveExpiredAlerts() {
        int archived = autoArchiveService.archiveExpired(ageInDays);
        if (archived > 0) {
            log.info("{} alerte(s) archivée(s) automatiquement après {} jours.", archived, ageInDays);
        }
    }
}
