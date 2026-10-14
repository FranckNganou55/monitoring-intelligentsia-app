package org.intelligentsia.monitoring.domain.monitoring.alert.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;

@Getter
@Setter
public class UpdateAlertStatusRequest {

    @NotNull
    private MonitoringAlert.AlertStatus status;

    @Size(max = 2000)
    private String comment;
}
