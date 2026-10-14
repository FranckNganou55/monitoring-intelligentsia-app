package org.intelligentsia.monitoring.domain.configuration.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateCountryCodeRequest {
    @NotBlank
    @Pattern(regexp = "[0-9]{1,20}")
    private String code;
}
