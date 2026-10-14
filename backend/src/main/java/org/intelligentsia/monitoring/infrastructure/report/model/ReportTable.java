package org.intelligentsia.monitoring.infrastructure.report.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ReportTable {

    @Builder.Default
    private final boolean recordLayout = false;

    private final String title;

    private final List<ReportColumn> columns;

    private final List<Map<String, Object>> rows;
}