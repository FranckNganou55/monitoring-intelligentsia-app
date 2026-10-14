package org.intelligentsia.monitoring.domain.monitoring.extraction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ExtractionCreateRequest {
    @NotNull private Long institutionId;
    @NotNull private String type;
    private Instant startDate;
    private Instant endDate;
    private List<String> terminalIds;
    private String merchant;
    private String city;
}
