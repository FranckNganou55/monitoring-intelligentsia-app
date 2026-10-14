package org.intelligentsia.monitoring.domain.extraction.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionConfig;
import org.intelligentsia.monitoring.domain.extraction.repository.ExtractionConfigRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionSchedulerService {

    private final TaskScheduler taskScheduler;
    private final ExtractionConfigRepository configRepository;
    private final ExtractionEngineService engineService;

    /** Map configId → running ScheduledFuture */
    private final Map<Long, ScheduledFuture<?>> scheduled = new ConcurrentHashMap<>();

    /** Load all active configs with a cron at startup. */
    @org.springframework.beans.factory.annotation.Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @PostConstruct
    public void loadAll() {
        if (!schedulingEnabled) return;
        List<ExtractionConfig> configs = configRepository.findByActiveTrueAndCronExpressionIsNotNull();
        configs.forEach(c -> schedule(c.getId(), c.getCronExpression()));
        log.info("ExtractionScheduler: {} tâche(s) planifiée(s) au démarrage", configs.size());
    }

    /** Schedule or re-schedule a config. Cancels any existing job first. */
    public void schedule(Long configId, String cronExpression) {
        if (!schedulingEnabled) return;
        cancel(configId);
        if (cronExpression == null || cronExpression.isBlank()) return;
        try {
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> runSafely(configId),
                    new CronTrigger(cronExpression));
            scheduled.put(configId, future);
            log.info("Extraction #{} planifiée avec le cron '{}'", configId, cronExpression);
        } catch (IllegalArgumentException e) {
            log.error("Expression cron invalide '{}' pour extraction #{}: {}", cronExpression, configId, e.getMessage());
        }
    }

    /** Remove cron scheduling for a config (e.g. when deactivated or deleted). */
    public void cancel(Long configId) {
        ScheduledFuture<?> existing = scheduled.remove(configId);
        if (existing != null) {
            existing.cancel(false);
            log.info("Extraction #{} dé-planifiée", configId);
        }
    }

    /** Returns true if configId currently has an active cron. */
    public boolean isScheduled(Long configId) {
        ScheduledFuture<?> f = scheduled.get(configId);
        return f != null && !f.isCancelled() && !f.isDone();
    }

    private void runSafely(Long configId) {
        try {
            engineService.triggerScheduled(configId);
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution planifiée de l'extraction #{}: {}", configId, e.getMessage(), e);
        }
    }
}
