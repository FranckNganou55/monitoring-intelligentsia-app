package org.intelligentsia.monitoring.infrastructure.report.generator;

import org.intelligentsia.monitoring.infrastructure.report.model.ReportDocument;

import java.io.IOException;
import java.nio.file.Path;


public interface ReportGenerator {

    String format();

    void generate(
            ReportDocument report,
            Path target
    ) throws IOException;
}