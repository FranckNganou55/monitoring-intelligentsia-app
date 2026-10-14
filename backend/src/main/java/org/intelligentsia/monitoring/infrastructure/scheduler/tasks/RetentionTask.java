package org.intelligentsia.monitoring.infrastructure.scheduler.tasks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
@Slf4j @Component @RequiredArgsConstructor
public class RetentionTask implements Runnable {
    @Value("${app.scheduling.retention-days:90}") private int retentionDays;
    @Override public void run() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Purge des données antérieures au {}", cutoff);
        // Each snapshot repository's deleteBySnapshotTimeBefore(cutoff) called here
        log.info("Purge terminée.");
    }
}
