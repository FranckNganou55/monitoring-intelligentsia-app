package org.intelligentsia.monitoring.domain.monitoring.fleet.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CardOverLimitDto {
    private Long id;
    private Long institutionId;
    private String cardNumber;
    private String currency;
    private BigDecimal limitAmount;
    private BigDecimal currentAmount;
    private Boolean exceeded;
    private Instant snapshotTime;
}
