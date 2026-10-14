package org.intelligentsia.monitoring.domain.monitoring.fraud.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FraudAlertOverviewDto {
    private int criticalCount;
    private int mediumCount;
    private int typicalCount;
    private int lessCriticalCount;
    /** Kept for older clients; this is the sum of typical and less-critical alerts. */
    private int lowCount;
    private int toQualifyCount;
    private int confirmedCount;
    private List<Bucket> hourlyAlerts;
    private List<Breakdown> topBins;
    private List<Breakdown> topRules;
    private List<AlertRow> alerts;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Getter
    @Builder
    public static class Bucket {
        private LocalDateTime time;
        private int count;
        private int criticalCount;
        private int mediumCount;
        private int typicalCount;
        private int lessCriticalCount;
    }

    @Getter
    @Builder
    public static class Breakdown {
        private String label;
        private String description;
        private long count;
    }

    @Getter
    @Builder
    public static class AlertRow {
        private Long id;
        private LocalDateTime detectedAt;
        private String riskLevel;
        private String cardNumber;
        private BigDecimal amount;
        private String currency;
        private String terminalId;
        private Long institutionId;
        private String institutionName;
        private Long ruleId;
        private String ruleCode;
        private String ruleName;
        private String title;
        private String status;
        private String transactionReference;
    }
}
