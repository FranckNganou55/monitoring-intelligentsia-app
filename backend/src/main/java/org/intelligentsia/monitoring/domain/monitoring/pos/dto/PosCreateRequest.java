package org.intelligentsia.monitoring.domain.monitoring.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PosCreateRequest {
    @NotNull private Long institutionId;
    @NotBlank @Size(max = 50) private String terminalId;
    @Size(max = 200) private String merchant;
    @Size(max = 100) private String city;
    @Size(max = 100) private String district;
    @Size(max = 200) private String extraName;
    private Long technicianId;
}
