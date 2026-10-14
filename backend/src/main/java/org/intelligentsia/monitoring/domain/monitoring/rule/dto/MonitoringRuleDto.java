package org.intelligentsia.monitoring.domain.monitoring.rule.dto;

import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MonitoringRuleDto {

    private Long id;

    private String code;

    private String name;

    private String description;

    private MonitoringRule.RuleCategory category;

    private MonitoringRule.RiskLevel riskLevel;

    private Double riskPercentage;

    private Boolean enabled;

    private Boolean notificationEnabled;

    private Boolean soundEnabled;

    private Boolean visualAlertEnabled;

    private Long alertCount;

    private Double alertPercentage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
