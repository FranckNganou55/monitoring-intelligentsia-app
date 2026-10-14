package org.intelligentsia.monitoring.domain.monitoring.fraud.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class FraudRateDto {
    private Long id;
    private Long institutionId;
    private String bin;
    private Integer suspectCount;
    private Integer fraudCount;
    private Instant snapshotTime;
}
