package org.intelligentsia.monitoring.domain.configuration.settings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppSettingUpdateRequest {
    @NotNull
    private String valeur;
}
