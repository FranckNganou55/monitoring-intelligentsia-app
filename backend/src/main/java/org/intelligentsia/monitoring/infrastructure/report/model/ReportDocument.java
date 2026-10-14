package org.intelligentsia.monitoring.infrastructure.report.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportDocument {

    private final ReportMetadata metadata;

    private final List<ReportKpi> kpis;

    private final List<ReportSection> sections;

    /** Keep related entity details and movements together in one Excel worksheet. */
    private final boolean groupedDetails;
}