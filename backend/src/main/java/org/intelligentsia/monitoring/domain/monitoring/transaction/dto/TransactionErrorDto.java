package org.intelligentsia.monitoring.domain.monitoring.transaction.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class TransactionErrorDto {
    private Long id;
    private String network;
    private String responseCode;
    private String responseDesc;
    private Integer count;
    private Instant snapshotTime;
}
