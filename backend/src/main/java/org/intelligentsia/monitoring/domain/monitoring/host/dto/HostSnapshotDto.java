package org.intelligentsia.monitoring.domain.monitoring.host.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class HostSnapshotDto {
    private Long id;
    private String hostname;
    private String description;
    private String ipAddress;
    private String operatingStatus;
    private String processingStatus;
    private Instant snapshotTime;
}
