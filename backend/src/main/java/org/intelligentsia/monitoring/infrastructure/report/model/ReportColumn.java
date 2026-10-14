package org.intelligentsia.monitoring.infrastructure.report.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportColumn {

    private final String key;
    private final String label;
}