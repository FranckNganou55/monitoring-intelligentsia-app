package org.intelligentsia.monitoring.domain.monitoring.network.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class NetworkSnapshotDto {
    private Long id;
    private String networkCode;
    private String description;
    private String operatingStatus;
    private String processingStatus;
    private String transactionMode;
    private Instant snapshotTime;
}
