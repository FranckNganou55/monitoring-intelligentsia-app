package org.intelligentsia.monitoring.domain.configuration.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestMailRequest {
    @NotBlank @Email
    private String to;
}
