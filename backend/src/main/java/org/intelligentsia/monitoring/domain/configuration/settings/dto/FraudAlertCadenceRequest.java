package org.intelligentsia.monitoring.domain.configuration.settings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FraudAlertCadenceRequest {
    @NotNull private Long typiqueMs;
    @NotNull private Long moinsCritiqueMs;
    @NotNull private Long moyenMs;
}
