package org.intelligentsia.monitoring.domain.monitoring.rule.dto;

import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitoringRuleRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 200)
    private String name;

    private String description;

    @NotNull
    private MonitoringRule.RuleCategory category;

    @NotNull
    private MonitoringRule.RiskLevel riskLevel;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double riskPercentage = 0.0;

    private Boolean enabled = true;

    private Boolean notificationEnabled = true;

    private Boolean soundEnabled = false;

    private Boolean visualAlertEnabled = false;
}
