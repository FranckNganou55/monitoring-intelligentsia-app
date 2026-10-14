package org.intelligentsia.monitoring.domain.monitoring.alert.dto;

import lombok.Builder;
import lombok.Getter;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;

import java.time.LocalDateTime;

@Getter
@Builder
public class MonitoringAlertDto {

    private Long id;

    private Long ruleId;

    private String ruleCode;

    private String ruleName;

    private MonitoringAlert.Category category;

    private MonitoringRule.RiskLevel riskLevel;

    private String title;

    private String description;

    private MonitoringAlert.AlertStatus status;

    private Long institutionId;

    private Long atmId;

    private Long fraudDetectionId;

    private String transactionReference;

    private LocalDateTime detectedAt;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime archivedAt;

    private Long assignedTo;
}