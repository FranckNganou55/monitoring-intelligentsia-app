package org.intelligentsia.monitoring.domain.monitoring.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TechnicianCreateRequest {
    @NotNull private Long institutionId;
    @NotBlank @Size(max = 200) private String name;
    @Size(max = 50) private String phone;
    private String cards;
}
