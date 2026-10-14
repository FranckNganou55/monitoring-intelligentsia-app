package org.intelligentsia.monitoring.domain.monitoring.transaction.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TransactionRateDto {
    private Long id;
    private String network;
    private String acquirer;
    private String issuer;
    private Integer successCount;
    private BigDecimal successRate;
    private Integer failureCount;
    private BigDecimal failureRate;
    private Instant snapshotTime;
}
