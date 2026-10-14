package org.intelligentsia.monitoring.infrastructure.report.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportKpi {

    private final String label;
    private final String value;
    private final String description;
}