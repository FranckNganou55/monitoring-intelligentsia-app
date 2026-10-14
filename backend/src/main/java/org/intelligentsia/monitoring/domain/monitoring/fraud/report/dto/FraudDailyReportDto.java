package org.intelligentsia.monitoring.domain.monitoring.fraud.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class FraudDailyReportDto {
    private Long id;
    private LocalDate reportDate;
    private int totalDetected;
    private int totalProcessed;
    private int totalUnprocessed;
    private LocalDateTime generatedAt;
    private String status;
    private String errorMessage;
    private boolean downloadable;
    private List<String> availableFormats;
}
