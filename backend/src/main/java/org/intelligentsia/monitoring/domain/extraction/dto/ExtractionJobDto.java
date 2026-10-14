package org.intelligentsia.monitoring.domain.extraction.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ExtractionJobDto {
    private Long id;
    private Long configId;
    private String configName;
    private Long institutionId;
    private String institutionName;
    private String triggerType;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private Long durationMs;
    private Integer rowCount;
    private boolean downloadable;
    private String errorMessage;
    private String triggeredByLogin;
    private Instant createdAt;
}
