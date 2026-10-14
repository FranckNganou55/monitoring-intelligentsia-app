package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.AtmDto;
import org.intelligentsia.monitoring.infrastructure.report.model.*;
import org.intelligentsia.monitoring.infrastructure.report.service.ReportExportService;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AtmReportService {
    private final AtmService atms;
    private final AtmMovementService movements;
    private final ReportExportService exports;

    @Transactional(readOnly = true)
    public byte[] export(Long id, Long institutionId, String search, String status,
                         String movementStatus, Instant from, Instant to, String format) throws IOException {
        List<AtmDto> selected = new ArrayList<>();
        if (id != null) selected.add(atms.findById(id, institutionId));
        else {
            FilterRequest filter = new FilterRequest();
            filter.setSearch(search);
            filter.setSize(100);
            filter.setSortBy("terminalId");
            filter.setDirection("ASC");
            while (true) {
                var page = atms.findAll(filter, institutionId, status);
                selected.addAll(page.getContent());
                if (page.isLast()) break;
                filter.setPage(filter.getPage() + 1);
            }
        }
        List<ReportSection> sections = new ArrayList<>();
        int movementCount = 0;
        for (AtmDto atm : selected) {
            List<Map<String, Object>> history = new ArrayList<>();
            for (var movement : movements.all(atm.getId(), institutionId, movementStatus, from, to)) {
                history.add(Map.of("date", java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(movement.observedAt().atZone(ZoneId.of("Africa/Lagos"))),
                        "source", movement.source(), "previous", text(movement.previousStatus()),
                        "status", movement.status() + " · " + stateLabel(movement.state()), "message", text(movement.message())));
            }
            movementCount += history.size();
            var block = section("GAB " + atm.getTerminalId(), "",
                    history, "date", "Date", "source", "Source", "previous", "Statut précédent",
                    "status", "Nouveau statut", "message", "Message");
            block = ReportSection.builder().title(block.getTitle()).description(block.getDescription()).table(block.getTable()).items(List.of(
                    Map.of("label", "Institution", "total", text(atm.getInstitutionName())),
                    Map.of("label", "Ville", "total", text(atm.getCity())),
                    Map.of("label", "Adresse", "total", text(atm.getAddress())),
                    Map.of("label", "Statut actuel", "total", text(atm.getStatusDesc() == null ? atm.getStatusId() : atm.getStatusDesc())))).build();
            sections.add(block);
        }
        var report = ReportDocument.builder().metadata(ReportMetadata.builder()
                .applicationName("MonitoringApp").institutionName(institutionId == null ? "Toutes les institutions autorisées" : "Institution " + institutionId)
                .reportTitle(id == null ? "Rapport des GABs et de leurs mouvements" : "Mouvements du GAB " + selected.get(0).getTerminalId())
                .subtitle("Recherche : " + text(search) + " · État GAB : " + text(status) + " · Mouvement : " + text(movementStatus))
                .period((from == null ? "Début de l’historique" : reportDate(from)) + " au " + (to == null ? "dernier relevé" : reportDate(to)) + " (Africa/Lagos)").source("Historique des relevés GAB et interfaces SVFE")
                .comment("Seuls les changements observés lors des collectes sont disponibles. La première ligne de chaque source est son état initial connu. Une borne absente inclut tout l’historique conservé.")
                .generatedAt(LocalDateTime.now(ZoneId.of("Africa/Lagos"))).build())
                .kpis(List.of(ReportKpi.builder().label("GABs sélectionnés").value("" + selected.size()).build(),
                        ReportKpi.builder().label("Mouvements observés").value("" + movementCount).build()))
                .sections(sections).groupedDetails(true).build();
        var target = Files.createTempFile("atm-report-", "." + format.toLowerCase(Locale.ROOT));
        try {
            exports.export(report, format, target);
            return Files.readAllBytes(target);
        } finally { Files.deleteIfExists(target); }
    }

    private ReportSection section(String title, String description, List<Map<String, Object>> rows, String... columns) {
        List<ReportColumn> fields = new ArrayList<>();
        for (int i = 0; i < columns.length; i += 2) fields.add(ReportColumn.builder().key(columns[i]).label(columns[i + 1]).build());
        return ReportSection.builder().title(title).description(description)
                .table(ReportTable.builder().title("Mouvements du " + title).columns(fields).rows(rows).build()).build();
    }

    private String reportDate(Instant value) { return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(value.atZone(ZoneId.of("Africa/Lagos"))); }

    private String text(Object value) { return value == null || value.toString().isBlank() ? "—" : value.toString(); }
    private String stateLabel(String state) { return switch (state) { case "ok" -> "Opérationnel"; case "warning" -> "Avertissement"; case "error" -> "Hors ligne"; default -> "Inconnu"; }; }
}
