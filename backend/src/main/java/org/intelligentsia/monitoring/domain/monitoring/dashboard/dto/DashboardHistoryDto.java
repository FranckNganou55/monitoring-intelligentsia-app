package org.intelligentsia.monitoring.domain.monitoring.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class DashboardHistoryDto {

    private List<AtmAvailabilityPoint> atmAvailabilityHistory;
    private List<AtmAlertImpactPoint> atmAlertImpact;
    private List<FraudHistoryPoint> fraudHistory;

    @Getter
    @Builder
    public static class AtmAvailabilityPoint {
        private Instant time;
        private double percentage;
    }

    @Getter
    @Builder
    public static class AtmAlertImpactPoint {
        private Instant time;
        private String terminalId;
        private int count;
    }

    @Getter
    @Builder
    public static class FraudHistoryPoint {
        private Instant time;
        private int count;
    }
}
