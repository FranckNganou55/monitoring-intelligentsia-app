package org.intelligentsia.monitoring.domain.configuration.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InstitutionCreateRequest {
    @NotBlank @Size(max = 50) private String code;
    @NotBlank @Size(max = 200) private String name;
    private String svfeId;
    private String hostPid;
    private String mailRecharge;
}
