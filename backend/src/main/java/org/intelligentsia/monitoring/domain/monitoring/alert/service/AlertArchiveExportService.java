package org.intelligentsia.monitoring.domain.monitoring.alert.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.alert.repository.MonitoringAlertRepository;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.report.model.*;
import org.intelligentsia.monitoring.infrastructure.report.service.ReportExportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertArchiveExportService {
    private final MonitoringAlertRepository alerts;
    private final ReportExportService exportService;
    private final org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository institutions;

    @Transactional(readOnly = true)
    public byte[] export(String requestedFormat, String search, MonitoringAlert.Category category,
                         MonitoringRule.RiskLevel riskLevel, Long institutionId,
                         LocalDateTime detectedFrom, LocalDateTime detectedUntil,
                         Integer year, Integer month, Integer day, Integer hour) throws IOException {
        return export(requestedFormat, search, category, riskLevel, institutionId, detectedFrom, detectedUntil, year, month, day, hour, false);
    }

    @Transactional(readOnly = true)
    public byte[] export(String requestedFormat, String search, MonitoringAlert.Category category,
                         MonitoringRule.RiskLevel riskLevel, Long institutionId,
                         LocalDateTime detectedFrom, LocalDateTime detectedUntil,
                         Integer year, Integer month, Integer day, Integer hour, boolean includeActive) throws IOException {
        String format = requestedFormat == null ? "" : requestedFormat.trim().toUpperCase(Locale.ROOT);
        if (!List.of("WORD", "PDF", "EXCEL").contains(format)) {
            throw new BusinessException("INVALID_FORMAT", "Les archives peuvent être exportées en Word, PDF ou Excel.");
        }

        List<MonitoringAlert> records = new ArrayList<>();
        int pageIndex = 0;
        Page<MonitoringAlert> page;
        do {
            page = alerts.findWithFilters(includeActive ? null : MonitoringAlert.AlertStatus.ARCHIVED, true,
                    MonitoringAlert.AlertStatus.ARCHIVED, category, riskLevel, null, institutionId,
                    null, search, detectedFrom, detectedUntil, year, month, day, hour,
                    PageRequest.of(pageIndex++, 500, Sort.by(Sort.Direction.DESC, "detectedAt")));
            records.addAll(page.getContent());
        } while (page.hasNext());

        Map<Long, String> institutionNames = new LinkedHashMap<>();
        var institutionIds = records.stream().map(MonitoringAlert::getInstitutionId).filter(java.util.Objects::nonNull).distinct().toList();
        institutions.findAllById(institutionIds).forEach(i -> institutionNames.put(i.getId(), i.getName() + " (" + i.getCode() + ")"));
        List<Map<String, Object>> rows = records.stream().map(alert -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", alert.getId());
            row.put("detected_at", alert.getDetectedAt());
            row.put("institution", alert.getInstitutionId() == null ? "—" : institutionNames.getOrDefault(alert.getInstitutionId(), "Institution " + alert.getInstitutionId()));
            row.put("category", alert.getCategory());
            row.put("risk", alert.getRiskLevel().name().replace('_', ' '));
            row.put("rule", alert.getRule() == null ? "—" : alert.getRule().getCode() + " — " + alert.getRule().getName());
            row.put("status", alert.getStatus());
            row.put("atm", alert.getAtm() == null ? "—" : alert.getAtm().getTerminalId());
            row.put("transaction", alert.getTransactionReference() == null ? "—" : alert.getTransactionReference());
            row.put("title", alert.getTitle());
            row.put("description", alert.getDescription() == null ? "—" : alert.getDescription());
            return row;
        }).toList();

        String period = periodLabel(year, month, day, hour, detectedFrom, detectedUntil);
        ReportDocument document = ReportDocument.builder()
                .metadata(ReportMetadata.builder()
                        .applicationName("Intelligentsia Monitoring")
                        .institutionName(institutionId == null ? "Toutes institutions" : institutionNames.getOrDefault(institutionId, "Institution " + institutionId))
                        .reportTitle("Archives des alertes")
                .subtitle(includeActive ? "Toutes les alertes : actives et archivées" : "Alertes archivées uniquement")
                        .period(period)
                        .source("PostgreSQL / Monitoring")
                        .generatedAt(LocalDateTime.now())
                        .build())
                .kpis(List.of(ReportKpi.builder().label("Alertes exportées")
                        .value(String.valueOf(rows.size())).description("Selon les filtres et le périmètre choisis").build(),
                        ReportKpi.builder().label("Alertes actives")
                                .value(String.valueOf(records.stream().filter(a -> a.getStatus() != MonitoringAlert.AlertStatus.ARCHIVED).count()))
                                .description("Alertes non archivées dans cet export").build(),
                        ReportKpi.builder().label("Alertes archivées")
                                .value(String.valueOf(records.stream().filter(a -> a.getStatus() == MonitoringAlert.AlertStatus.ARCHIVED).count()))
                                .description("Historique archivé dans cet export").build(),
                        ReportKpi.builder().label("Risque critique")
                                .value(String.valueOf(records.stream().filter(a -> a.getRiskLevel() == MonitoringRule.RiskLevel.CRITIQUE).count()))
                                .description("Alertes à examiner en priorité").build()))
                .sections(List.of(ReportSection.builder().title("Historique des alertes")
                        .table(ReportTable.builder().title("Détail des alertes")
                                .columns(List.of(column("id", "ID"), column("detected_at", "Date et heure"),
                                        column("institution", "Institution"), column("category", "Catégorie"),
                                        column("risk", "Niveau"), column("rule", "Règle"),
                                        column("status", "Statut"), column("atm", "GAB"),
                                        column("transaction", "Transaction"), column("title", "Alerte"),
                                        column("description", "Détail")))
                                .rows(rows).build()).build()))
                .build();

        Path temporary = Files.createTempFile("monitoring-alert-archives-", format.equals("PDF") ? ".pdf" : format.equals("EXCEL") ? ".xlsx" : ".docx");
        try {
            exportService.export(document, format, temporary);
            return Files.readAllBytes(temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private ReportColumn column(String key, String label) {
        return ReportColumn.builder().key(key).label(label).build();
    }

    private String periodLabel(Integer year, Integer month, Integer day, Integer hour,
                               LocalDateTime from, LocalDateTime until) {
        if (from != null || until != null) return (from == null ? "Début non limité" : org.intelligentsia.monitoring.infrastructure.report.generator.ReportDisplay.value(from)) + " – " + (until == null ? "Fin non limitée" : org.intelligentsia.monitoring.infrastructure.report.generator.ReportDisplay.value(until));
        StringBuilder label = new StringBuilder();
        if (year != null) label.append(year);
        if (month != null) label.append(String.format("-%02d", month));
        if (day != null) label.append(String.format("-%02d", day));
        if (hour != null) label.append(String.format(" · %02d:00–%02d:59", hour, hour));
        return label.length() == 0 ? "Toutes les dates" : label.toString();
    }
}
