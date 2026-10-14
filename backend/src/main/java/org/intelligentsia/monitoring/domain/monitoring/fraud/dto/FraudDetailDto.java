package org.intelligentsia.monitoring.domain.monitoring.fraud.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class FraudDetailDto {
    private Long id;
    private Long institutionId;
    private String institutionName;
    private String bin;
    private String ruleCode;
    private String ruleDesc;
    private String sourceReference;
    private String sourceRiskLevel;
    private String riskLevel;
    private Integer suspectCount;
    private Integer fraudCount;
    private String bins;
    private String description;
    private Instant snapshotTime;
}
