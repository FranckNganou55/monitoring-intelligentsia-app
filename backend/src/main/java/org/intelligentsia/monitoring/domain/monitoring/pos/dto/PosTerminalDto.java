package org.intelligentsia.monitoring.domain.monitoring.pos.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class PosTerminalDto {
    private Long id;
    private Long institutionId;
    private String institutionName;
    private String terminalId;
    private String merchant;
    private String city;
    private String district;
    private String extraName;
    private Long technicianId;
    private String technicianName;
    private Instant createdAt;
    private Instant updatedAt;
}
