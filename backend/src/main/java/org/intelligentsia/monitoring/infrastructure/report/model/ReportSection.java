package org.intelligentsia.monitoring.infrastructure.report.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ReportSection {

    private final String title;

    private final String description;

    private final List<Map<String, Object>> items;

    private final ReportTable table;
}
