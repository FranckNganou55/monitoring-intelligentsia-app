package org.intelligentsia.monitoring.infrastructure.report.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.report.generator.ReportGenerator;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final List<ReportGenerator> generators;

    public void export(
            ReportDocument report,
            String format,
            Path target
    ) throws IOException {

        String normalized = format == null || format.isBlank()
                ? "WORD"
                : format.trim().toUpperCase(Locale.ROOT);

        ReportGenerator generator =
                generators.stream()
                        .filter(item ->
                                item.format().equals(normalized))
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "INVALID_FORMAT",
                                        "Format de rapport non pris en charge : "
                                                + normalized
                                )
                        );

        generator.generate(report, target);
    }
}
