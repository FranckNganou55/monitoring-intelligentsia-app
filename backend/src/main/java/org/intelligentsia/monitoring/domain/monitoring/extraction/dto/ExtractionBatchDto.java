package org.intelligentsia.monitoring.domain.monitoring.extraction.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ExtractionBatchDto {
    private Long id;
    private Long institutionId;
    private String institutionName;
    private String type;
    private Instant startDate;
    private Instant endDate;
    private String merchant;
    private String city;
    private Instant createdAt;
    private Long createdBy;
    private List<String> terminalIds;
    private long transactionCount;
}
