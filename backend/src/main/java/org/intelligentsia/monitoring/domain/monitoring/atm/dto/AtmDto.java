package org.intelligentsia.monitoring.domain.monitoring.atm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class AtmDto {
    private Long id;
    private Long institutionId;
    private String institutionName;
    private String terminalId;
    private String address;
    private String city;
    private Long currency1Id;
    private String currency1Alpha;
    private BigDecimal threshold1;
    private Long currency2Id;
    private String currency2Alpha;
    private BigDecimal threshold2;
    private Long svfePid;
    private Instant createdAt;
    private Instant updatedAt;
    // Last snapshot status (denormalized for list view)
    private String statusId;
    private String statusDesc;
    private Boolean allOk;
    private boolean threshold1Exceeded;
    private boolean threshold2Exceeded;
    private Integer quantity1;
    private Integer quantity2;
    private Integer quantity3;
    private Integer quantity4;
}
