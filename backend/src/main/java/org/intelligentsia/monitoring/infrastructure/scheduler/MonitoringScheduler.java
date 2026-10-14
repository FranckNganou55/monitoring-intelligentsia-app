package org.intelligentsia.monitoring.infrastructure.scheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.scheduler.entity.SchedulerTask;
import org.intelligentsia.monitoring.domain.configuration.scheduler.repository.SchedulerTaskRepository;
import org.intelligentsia.monitoring.infrastructure.scheduler.tasks.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Optional;

@Slf4j @Component @RequiredArgsConstructor
public class MonitoringScheduler {
    @org.springframework.beans.factory.annotation.Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;
    private final SchedulerTaskRepository taskRepo;
    private final org.intelligentsia.monitoring.domain.configuration.settings.service.AppSettingService settings;
    private final AtmQueryTask atmTask;
    private final NetworkQueryTask networkTask;
    private final HostQueryTask hostTask;
    private final TransactionQueryTask transactionTask;
    private final FraudQueryTask fraudTask;
    private final FleetQueryTask fleetTask;
    private final TeleCollectQueryTask teleCollectTask;
    private final RetentionTask retentionTask;

    @Scheduled(fixedDelay=60000)
    public void runAtm()         { runTask("ATM_QUERY", atmTask); }
    @Scheduled(fixedDelay=60000)
    public void runNetwork()     { runTask("NETWORK_QUERY", networkTask); }
    @Scheduled(fixedDelay=60000)
    public void runHost()        { runTask("HOST_QUERY", hostTask); }
    @Scheduled(fixedDelay=60000)
    public void runTransaction()  { runTask("TRANSACTION_QUERY", transactionTask); }
    // Check frequently; the persisted scheduler interval sets the actual FRAUD_QUERY cadence.
    @Scheduled(fixedDelay=5000)
    public void runFraud()       { runTask("FRAUD_QUERY", fraudTask); }
    @Scheduled(fixedDelay=60000)
    public void runFleet()       { runTask("FLEET_QUERY", fleetTask); }
    @Scheduled(cron="0 0 23 * * *")
    public void runTeleCollect() { runTask("TELECOLLECT_QUERY", teleCollectTask); }
    @Scheduled(cron="0 0 2 * * *")
    public void runRetention()   { runTask("RETENTION", retentionTask); }

    private void runTask(String code, Runnable task) {
        if (!schedulingEnabled) return;
        Optional<SchedulerTask> cfg = taskRepo.findByCode(code);
        if (cfg.isEmpty() || !cfg.get().isEnabled()) return;
        SchedulerTask t = cfg.get();
        long lastRun = t.getLastRun() != null ? t.getLastRun().toEpochMilli() : 0;
        long interval = t.getIntervalMs();
        if ("ATM_QUERY".equals(code)) interval = Math.min(interval, Math.min(
                settings.getInteger("alerts.atm.interface.DOWN.interval.ms", (int) interval),
                settings.getInteger("alerts.atm.interface.SIGNOFF.interval.ms", (int) interval)));
        if (System.currentTimeMillis() - lastRun < Math.max(60000, interval) && !"FRAUD_QUERY".equals(code)) return;
        if ("FRAUD_QUERY".equals(code) && System.currentTimeMillis() - lastRun < interval) return;
        try {
            t.setLastStatus("RUNNING"); t.setLastRun(Instant.now()); taskRepo.save(t);
            task.run();
            t.setLastStatus("SUCCESS"); t.setLastError(null);
        } catch (Exception e) {
            log.error("Erreur tâche {}: {}", code, e.getMessage());
            t.setLastStatus("ERROR"); t.setLastError(e.getMessage());
        } finally {
            taskRepo.save(t);
        }
    }
}
