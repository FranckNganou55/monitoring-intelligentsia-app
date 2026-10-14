package org.intelligentsia.monitoring.domain.configuration.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "scheduler_tasks") @Getter @Setter @NoArgsConstructor
public class SchedulerTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "interval_ms", nullable = false) private long intervalMs;
    @Column(name = "last_run") private Instant lastRun;
    @Column(name = "last_status", length = 20) private String lastStatus;
    @Column(name = "last_error") private String lastError;
}
