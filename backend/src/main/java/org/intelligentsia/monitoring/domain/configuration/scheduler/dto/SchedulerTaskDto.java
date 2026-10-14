package org.intelligentsia.monitoring.domain.configuration.scheduler.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class SchedulerTaskDto {
    private Long id;
    private String code;
    private String name;
    private boolean enabled;
    private long intervalMs;
    private Instant lastRun;
    private String lastStatus;
    private String lastError;
}
