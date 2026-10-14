package org.intelligentsia.monitoring.domain.monitoring.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import org.intelligentsia.monitoring.domain.monitoring.host.dto.HostSnapshotDto;
import org.intelligentsia.monitoring.domain.monitoring.network.dto.NetworkSnapshotDto;

import java.util.List;

@Getter
@Builder
public class DashboardSummaryDto {
    private int totalAtms;
    private int atmsOnline;
    private int atmsOffline;
    private int atmsWarning;
    private int atmsUnknown;
    private int totalPos;
    private int posWithoutTelecollect;
    private int gabAlertsToday;
    private int totalFrauds;
    private Double transactionSuccessRate;
    private List<NetworkSnapshotDto> networks;
    private List<HostSnapshotDto> hosts;
}
