package org.intelligentsia.monitoring.domain.monitoring.atm.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AtmCreateRequest {
    @NotNull private Long institutionId;
    @NotBlank @Size(max = 50) private String terminalId;
    private String address;
    @Size(max = 100) private String city;
    private Long currency1Id;
    @DecimalMin("0") private BigDecimal threshold1;
    private Long currency2Id;
    @DecimalMin("0") private BigDecimal threshold2;
    private Long svfePid;
}
