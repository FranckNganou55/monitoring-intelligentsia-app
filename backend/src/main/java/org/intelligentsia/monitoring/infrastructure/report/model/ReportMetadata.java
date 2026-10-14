package org.intelligentsia.monitoring.infrastructure.report.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportMetadata {

    private final String applicationName;

    private final String institutionName;

    private final String reportTitle;

    private final String subtitle;

    private final String period;

    private final String source;

    private final String comment;

    private final LocalDateTime generatedAt;
}
