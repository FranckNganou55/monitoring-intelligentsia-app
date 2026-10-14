package org.intelligentsia.monitoring.domain.monitoring.alert.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;

@Getter
@Setter
public class CreateAlertRequest {

    @NotNull
    private Long ruleId;

    @NotNull
    private MonitoringAlert.Category category;

    private String title;

    private String description;

    private Long institutionId;

    private Long atmId;

    private Long fraudDetectionId;

    private String transactionReference;
}