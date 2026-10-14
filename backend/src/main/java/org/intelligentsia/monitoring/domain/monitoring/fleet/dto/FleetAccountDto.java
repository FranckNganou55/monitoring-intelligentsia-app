package org.intelligentsia.monitoring.domain.monitoring.fleet.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class FleetAccountDto {
    private Long id;
    private Long institutionId;
    private String accountNumber;
    private String currency;
    private BigDecimal balance;
    private BigDecimal previousBalance;
    private BigDecimal delta1;
    private BigDecimal delta2;
    private Boolean delta1Exceeded;
    private Boolean delta2Exceeded;
    private Instant snapshotTime;
}
