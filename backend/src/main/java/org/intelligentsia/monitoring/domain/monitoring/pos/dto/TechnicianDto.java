package org.intelligentsia.monitoring.domain.monitoring.pos.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class TechnicianDto {
    private Long id;
    private Long institutionId;
    private String institutionName;
    private String name;
    private String phone;
    private String cards;
    private Instant createdAt;
    private Instant updatedAt;
}
