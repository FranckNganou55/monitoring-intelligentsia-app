package org.intelligentsia.monitoring.domain.monitoring.fraud.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.monitoring.fraud.report.dto.FraudDailyReportDto;
import org.intelligentsia.monitoring.domain.monitoring.fraud.report.entity.FraudDailyReport;
import org.intelligentsia.monitoring.domain.monitoring.fraud.report.repository.FraudDailyReportRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportColumn;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportDocument;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportKpi;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportMetadata;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportSection;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportTable;
import org.intelligentsia.monitoring.infrastructure.report.service.ReportExportService;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.CmsCardLimitRepository;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDailyReportService {

    private static final List<String> FORMATS = List.of("WORD", "PDF", "EXCEL");
    private final FraudDailyReportRepository reports;
    private final NamedParameterJdbcTemplate jdbc;
    private final ReportExportService reportExportService;
    private final Optional<CmsCardLimitRepository> cmsCardLimitRepository;

    @Value("${app.reports.output-dir:${java.io.tmpdir}/fraud-reports}")
    private String outputDir;

    @Transactional(readOnly = true)
    public PageResponse<FraudDailyReportDto> list(FilterRequest filter) {
        return PageResponse.of(
                reports.findAllByOrderByReportDateDesc(filter.toPageable()).map(this::toDto)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<FraudDailyReportDto> list(FilterRequest filter, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) return list(filter);
        LocalDate start = fromDate == null ? LocalDate.of(1900, 1, 1) : fromDate;
        LocalDate end = toDate == null ? LocalDate.of(9999, 12, 31) : toDate;
        if (start.isAfter(end)) throw new BusinessException("INVALID_REPORT_PERIOD", "La date de début doit précéder la date de fin.");
        return PageResponse.of(reports.findInPeriod(start, end, filter.toPageable()).map(this::toDto));
    }

    /** Period exports do not overwrite the report stored for a single day. */
    @Transactional(readOnly = true)
    public byte[] exportPeriod(LocalDate fromDate, LocalDate toDate, String requestedFormat, String comment,
                               LocalTime fromTime, LocalTime untilTime) throws IOException {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate))
            throw new BusinessException("INVALID_REPORT_PERIOD", "Choisissez une date de début et une date de fin valides.");
        String format = normalizeFormat(requestedFormat);
        validateFormat(format);
        LocalDateTime from = fromDate.atTime(fromTime == null ? LocalTime.MIDNIGHT : fromTime);
        LocalDateTime until = untilTime == null ? toDate.plusDays(1).atStartOfDay() : toDate.atTime(untilTime).plusMinutes(1);
        if (!from.isBefore(until)) throw new BusinessException("INVALID_REPORT_PERIOD", "Le début doit précéder la fin de la période.");
        Map<String, Object> counts = jdbc.queryForMap("""
                SELECT COUNT(*) AS total_detected,
                       COUNT(*) FILTER (WHERE status <> 'OPEN') AS total_processed,
                       COUNT(*) FILTER (WHERE status = 'OPEN') AS total_unprocessed
                FROM monitoring_alerts
                WHERE category = 'FRAUD' AND detected_at >= :from AND detected_at < :until AND deleted_at IS NULL
                """, parameters(from, until));
        FraudDailyReport report = new FraudDailyReport();
        report.setReportDate(fromDate);
        report.setTotalDetected(number(counts, "total_detected"));
        report.setTotalProcessed(number(counts, "total_processed"));
        report.setTotalUnprocessed(number(counts, "total_unprocessed"));
        report.setGeneratedAt(LocalDateTime.now());
        Path temporary = Files.createTempFile("fraud-period-", "." + format.toLowerCase(Locale.ROOT));
        try {
            reportExportService.export(buildReportDocument(report, comment, from, until), format, temporary);
            return Files.readAllBytes(temporary);
        } finally { Files.deleteIfExists(temporary); }
    }

    public FraudDailyReportDto generate(LocalDate date, String requestedFormat) {
        return generate(date, requestedFormat, null);
    }

    @Transactional
    public FraudDailyReportDto generate(LocalDate date, String requestedFormat, String comment) {
        return generate(date, requestedFormat, comment, null, null);
    }

    @Transactional
    public FraudDailyReportDto generate(LocalDate date, String requestedFormat, String comment,
                                       LocalTime fromTime, LocalTime untilTime) {
        String format = normalizeFormat(requestedFormat);
        validateFormat(format);
        LocalDate day = date == null ? LocalDate.now() : date;
        LocalDateTime from = fromTime == null ? day.atStartOfDay() : day.atTime(fromTime);
        LocalDateTime until = untilTime == null ? day.plusDays(1).atStartOfDay()
                : day.atTime(untilTime).plusMinutes(1);
        if (!from.isBefore(until)) {
            throw new BusinessException("INVALID_REPORT_PERIOD", "L’heure de début doit précéder l’heure de fin.");
        }
        Map<String, Object> counts = jdbc.queryForMap("""
                SELECT COUNT(*) AS total_detected,
                       COUNT(*) FILTER (WHERE status <> 'OPEN') AS total_processed,
                       COUNT(*) FILTER (WHERE status = 'OPEN') AS total_unprocessed
                FROM monitoring_alerts
                WHERE category = 'FRAUD' AND detected_at >= :from AND detected_at < :until AND deleted_at IS NULL
                """, parameters(from, until));

        FraudDailyReport report = reports.findByReportDate(day).orElseGet(FraudDailyReport::new);
        report.setReportDate(day);
        report.setTotalDetected(number(counts, "total_detected"));
        report.setTotalProcessed(number(counts, "total_processed"));
        report.setTotalUnprocessed(number(counts, "total_unprocessed"));
        report.setGeneratedAt(LocalDateTime.now());
        report.setStatus("GENERATED");
        report.setErrorMessage(null);
        report = reports.saveAndFlush(report);

        try {
            writeDocument(report, format, comment, from, until);
        } catch (IOException ex) {
            log.error("Échec de création du document pour le rapport fraude du {}", day, ex);
            report.setStatus("FAILED");
            report.setErrorMessage(ex.getMessage());
            report = reports.save(report);
        }
        return toDto(report);
    }

    @Transactional(readOnly = true)
    public Path getDocument(Long id, String requestedFormat) {
        FraudDailyReport report = reports.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FraudDailyReport", id));
        String format = normalizeFormat(requestedFormat);
        validateFormat(format);
        Path path = documentPath(report.getReportDate(), format);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("FILE_NOT_AVAILABLE", "Le document de ce rapport n'est pas disponible.");
        }
        return path;
    }

    private void writeDocument(FraudDailyReport report, String format, String comment,
                               LocalDateTime from, LocalDateTime until) throws IOException {
        Path directory = Path.of(outputDir);
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "fraud-report-", ".tmp");
        try {
            ReportDocument document = buildReportDocument(report, comment, from, until);
            reportExportService.export(document, format, temporary);
            moveDocument(temporary, documentPath(report.getReportDate(), format));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void moveDocument(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ReportDocument buildReportDocument(FraudDailyReport report, String comment,
                                               LocalDateTime from, LocalDateTime until) {
        LocalDate day = report.getReportDate();
        int detected = report.getTotalDetected();
        int processed = report.getTotalProcessed();
        int waiting = report.getTotalUnprocessed();
        double processingRate = detected == 0 ? 0.0 : processed * 100.0 / detected;

        List<ReportKpi> kpis = List.of(
                kpi("Alertes détectées", detected, "Nombre total d’alertes détectées"),
                kpi("Alertes prises en charge", processed, "Alertes dont le statut n’est plus ouvert"),
                kpi("Alertes en attente", waiting, "Alertes actuellement ouvertes"),
                ReportKpi.builder().label("Taux de traitement")
                        .value(String.format(Locale.ROOT, "%.1f %%", processingRate))
                        .description("Part des alertes traitées").build()
        );

        List<Map<String, Object>> rawRisks = queryRiskBreakdown(from, until);
        List<Map<String, Object>> riskBreakdown = completeRiskBreakdown(rawRisks);
        long critical = countFor(rawRisks, "CRITIQUE");
        long medium = countFor(rawRisks, "MOYEN");
        String attention = critical == 0 && medium == 0
                ? "Aucune alerte de niveau moyen ou critique pour cette période."
                : critical + " alerte(s) critique(s) et " + medium
                        + " alerte(s) de niveau moyen nécessitent une attention prioritaire.";

        List<ReportSection> sections = new ArrayList<>();
        sections.add(ReportSection.builder().title("Répartition par niveau de risque")
                .items(riskBreakdown).build());
        sections.add(ReportSection.builder().title("Répartition par statut")
                .items(queryStatusBreakdown(from, until)).build());
        sections.add(ReportSection.builder().title("Règles déclenchées")
                .description("Chaque règle est conservée séparément, y compris lorsque plusieurs règles partagent le même code.")
                .table(buildRulesTable(queryTopRules(from, until))).build());
        sections.add(ReportSection.builder().title("Points d’attention")
                .description(attention).build());
        List<Map<String, Object>> alertRows = queryAlerts(from, until);
        sections.add(ReportSection.builder().title("Alertes enregistrées")
                .description("Alertes de fraude détectées pendant la période sélectionnée.")
                .table(buildAlertsContextTable(alertRows)).build());
        sections.add(ReportSection.builder().title("Commentaires des alertes")
                .description("Tous les commentaires de suivi associés aux alertes de fraude de la période, avec leur date et leur auteur.")
                .table(buildAlertCommentsTable(queryAlertComments(from, until))).build());
        sections.add(ReportSection.builder().title("Cartes et transactions")
                .description("Détails carte et transaction associés aux alertes enregistrées. Les numéros de carte sont masqués.")
                .table(buildCardTransactionTable(alertRows)).build());
        sections.add(ReportSection.builder().title("Suivi des traitements")
                .description("État de prise en charge des alertes et affectation connue.")
                .table(buildTreatmentTable(alertRows)).build());
        sections.add(ReportSection.builder().title("Validation")
                .table(buildValidationTable()).build());

        ReportMetadata metadata = ReportMetadata.builder()
                .applicationName("Intelligentsia Monitoring")
                .institutionName(queryInstitutionName(from, until))
                .reportTitle(from.toLocalDate().equals(until.minusSeconds(1).toLocalDate()) ? "Rapport journalier des alertes fraude" : "Rapport des alertes fraude sur la période")
                .subtitle("Synthèse et analyse opérationnelle des alertes détectées")
                .period("Du " + org.intelligentsia.monitoring.infrastructure.report.generator.ReportDisplay.value(from)
                        + " au " + org.intelligentsia.monitoring.infrastructure.report.generator.ReportDisplay.value(until.minusSeconds(1)))
                .source("PostgreSQL / Monitoring")
                .generatedAt(report.getGeneratedAt())
                .comment(comment == null || comment.isBlank() ? null : comment.trim())
                .build();

        return ReportDocument.builder().metadata(metadata).kpis(kpis).sections(sections).build();
    }

    private ReportKpi kpi(String label, int value, String description) {
        return ReportKpi.builder().label(label).value(String.valueOf(value)).description(description).build();
    }

    private String queryInstitutionName(LocalDateTime from, LocalDateTime until) {
        List<String> names = jdbc.queryForList("""
                SELECT DISTINCT i.name
                FROM monitoring_alerts a
                JOIN institutions i ON i.id = a.institution_id
                WHERE a.category = 'FRAUD' AND a.detected_at >= :from AND a.detected_at < :until
                  AND a.deleted_at IS NULL
                ORDER BY i.name
                """, parameters(from, until), String.class);
        if (names.isEmpty()) return "Toutes institutions";
        return names.size() == 1 ? names.get(0) : "Plusieurs institutions";
    }

    private List<Map<String, Object>> queryRiskBreakdown(LocalDateTime from, LocalDateTime until) {
        return jdbc.queryForList("""
                SELECT risk_level AS label, COUNT(*) AS total
                FROM monitoring_alerts
                WHERE category = 'FRAUD' AND detected_at >= :from AND detected_at < :until AND deleted_at IS NULL
                GROUP BY risk_level ORDER BY COUNT(*) DESC, risk_level
                """, parameters(from, until));
    }

    private List<Map<String, Object>> completeRiskBreakdown(List<Map<String, Object>> actual) {
        List<String> levels = List.of("CRITIQUE", "MOYEN", "MOINS_CRITIQUE", "TYPIQUE");
        return levels.stream().map(level -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", displayRiskLevel(level));
            row.put("total", countFor(actual, level));
            return row;
        }).toList();
    }

    private String displayRiskLevel(String level) {
        return level == null ? "—" : level.replace('_', ' ');
    }

    private List<Map<String, Object>> queryStatusBreakdown(LocalDateTime from, LocalDateTime until) {
        return jdbc.queryForList("""
                SELECT status AS label, COUNT(*) AS total
                FROM monitoring_alerts
                WHERE category = 'FRAUD' AND detected_at >= :from AND detected_at < :until AND deleted_at IS NULL
                GROUP BY status ORDER BY COUNT(*) DESC, status
                """, parameters(from, until));
    }

    private List<Map<String, Object>> queryTopRules(LocalDateTime from, LocalDateTime until) {
        return jdbc.queryForList("""
                SELECT REPLACE(a.risk_level, '_', ' ') AS risk_level,
                       COALESCE(r.code, 'N/A') AS rule_code,
                       COALESCE(r.id::text, '—') AS rule_id,
                       COALESCE(r.name, 'Règle supprimée') AS rule_name,
                       COUNT(*) AS total
                FROM monitoring_alerts a
                LEFT JOIN monitoring_rules r ON r.id = a.rule_id
                WHERE a.category = 'FRAUD' AND a.detected_at >= :from AND a.detected_at < :until AND a.deleted_at IS NULL
                GROUP BY a.risk_level, r.id, r.code, r.name
                ORDER BY CASE a.risk_level WHEN 'CRITIQUE' THEN 0 WHEN 'MOYEN' THEN 1
                         WHEN 'MOINS_CRITIQUE' THEN 2 ELSE 3 END, COUNT(*) DESC, r.code, r.id
                LIMIT 20
                """, parameters(from, until));
    }

    private ReportTable buildRulesTable(List<Map<String, Object>> rows) {
        return ReportTable.builder().title("Règles")
                .columns(List.of(column("risk_level", "Niveau"), column("rule_code", "Code"), column("rule_id", "ID règle"),
                        column("rule_name", "Règle déclenchée"), column("total", "Alertes")))
                .rows(rows).build();
    }

    private ReportTable buildAlertsContextTable(List<Map<String, Object>> rows) {
        List<ReportColumn> columns = List.of(
                column("id", "ID"),
                column("detected_at", "Détection"),
                column("risk_level", "Risque"),
                column("institution_name", "Institution"),
                column("rule_label", "Règle déclenchée"),
                column("status", "Statut"),
                column("title", "Alerte")
        );
        return ReportTable.builder().recordLayout(true).title("Alertes enregistrées")
                .columns(columns).rows(rows).build();
    }

    private ReportTable buildCardTransactionTable(List<Map<String, Object>> rows) {
        return ReportTable.builder().title("Cartes et transactions")
                .columns(List.of(column("detected_at", "Date"), column("risk_level", "Niveau"),
                        column("card_number", "Carte masquée"), column("amount", "Montant"),
                        column("terminal_id", "Terminal / GAB"), column("transaction_reference", "Référence transaction"),
                        column("merchant_name", "Commerçant"), column("transaction_type", "Type de transaction")))
                .rows(rows).build();
    }

    private ReportTable buildAlertCommentsTable(List<Map<String, Object>> rows) {
        return ReportTable.builder().title("Commentaires associés aux alertes")
                .columns(List.of(column("alert_id", "ID alerte"), column("title", "Alerte"),
                        column("commented_at", "Date du commentaire"), column("author", "Auteur"),
                        column("comment", "Commentaire")))
                .rows(rows).build();
    }

    private ReportTable buildTreatmentTable(List<Map<String, Object>> alerts) {
        List<Map<String, Object>> rows = alerts.stream().map(alert -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", alert.get("id"));
            row.put("terminal_id", alert.get("terminal_id"));
            row.put("rule_label", alert.get("rule_label"));
            row.put("treatment_state", alert.get("treatment_state"));
            row.put("assigned_to", alert.get("assigned_to"));
            return row;
        }).toList();
        return ReportTable.builder().title("Traitements")
                .columns(List.of(column("id", "ID alerte"), column("terminal_id", "Terminal / GAB"),
                        column("rule_label", "Règle"), column("treatment_state", "État du traitement"),
                        column("assigned_to", "Affectation")))
                .rows(rows).build();
    }

    private ReportTable buildValidationTable() {
        Map<String, Object> signatures = new LinkedHashMap<>();
        signatures.put("analyst", "");
        signatures.put("monitoring_manager", "");
        signatures.put("risk_manager", "");
        return ReportTable.builder().title("Validation du rapport")
                .columns(List.of(column("analyst", "Analyste fraude"),
                        column("monitoring_manager", "Responsable monitoring"),
                        column("risk_manager", "Responsable des risques")))
                .rows(List.of(signatures)).build();
    }

    private ReportColumn column(String key, String label) {
        return ReportColumn.builder().key(key).label(label).build();
    }

    private List<Map<String, Object>> queryAlerts(LocalDateTime from, LocalDateTime until) {
        List<Map<String, Object>> alerts = jdbc.queryForList("""
                SELECT a.id, a.detected_at, a.category, a.risk_level,
                       i.name AS institution_name, r.code AS rule_code, r.name AS rule_name,
                       COALESCE(r.code, 'N/A') || ' — ' || COALESCE(r.name, 'Règle supprimée') AS rule_label,
                       NULL::text AS customer_name, fd.account_reference AS card_candidate,
                       COALESCE(fd.amount::text || ' ' || fd.currency, fd.amount::text, 'Non renseigné') AS amount,
                       fd.merchant_name, fd.transaction_type,
                       a.title, a.status, atm.terminal_id, a.transaction_reference,
                       CASE a.status WHEN 'OPEN' THEN 'À traiter'
                            WHEN 'ACKNOWLEDGED' THEN 'Prise en compte'
                            WHEN 'IN_PROGRESS' THEN 'En cours'
                            WHEN 'RESOLVED' THEN 'Résolue'
                            WHEN 'ARCHIVED' THEN 'Archivée' ELSE a.status END AS treatment_state,
                       COALESCE(a.assigned_to::text, 'Non assignée') AS assigned_to
                FROM monitoring_alerts a
                LEFT JOIN monitoring_rules r ON r.id = a.rule_id
                LEFT JOIN institutions i ON i.id = a.institution_id
                LEFT JOIN atms atm ON atm.id = a.atm_id
                LEFT JOIN fraud_detections fd ON fd.id = a.fraud_detection_id
                WHERE a.category = 'FRAUD' AND a.detected_at >= :from AND a.detected_at < :until AND a.deleted_at IS NULL
                ORDER BY a.detected_at, a.id
                """, parameters(from, until));

        List<Map<String, Object>> sanitized = new ArrayList<>(alerts.size());
        Map<String, String> cardholderNames = new HashMap<>();
        for (Map<String, Object> alert : alerts) {
            Map<String, Object> row = new LinkedHashMap<>(alert);
            Object cardCandidate = row.remove("card_candidate");
            String cardSource = cardCandidate == null ? null : cardCandidate.toString();
            row.put("card_number", maskPan(cardSource));
            String cardDigits = cardDigits(cardSource);
            String customer = cardDigits == null
                    ? "Non renseigné"
                    : cardholderNames.computeIfAbsent(cardDigits, this::lookupCardholderName);
            row.put("customer_name", customer);
            sanitized.add(row);
        }
        return sanitized;
    }

    private List<Map<String, Object>> queryAlertComments(LocalDateTime from, LocalDateTime until) {
        return jdbc.queryForList("""
                SELECT a.id AS alert_id, a.title,
                       to_char(ae.created_at, 'DD/MM/YYYY HH24:MI:SS') AS commented_at,
                       COALESCE(NULLIF(trim(actor.login), ''), 'Système') AS author,
                       ae.comment
                FROM monitoring_alerts a
                JOIN alert_events ae ON ae.alert_id = a.id
                LEFT JOIN users actor ON actor.id = ae.actor_user_id
                WHERE a.category = 'FRAUD'
                  AND a.detected_at >= :from AND a.detected_at < :until
                  AND a.deleted_at IS NULL
                  AND ae.comment IS NOT NULL AND trim(ae.comment) <> ''
                ORDER BY a.detected_at, a.id, ae.created_at, ae.id
                """, parameters(from, until));
    }

    private String lookupCardholderName(String cardNumber) {
        if (cmsCardLimitRepository.isEmpty()) return "Non renseigné";
        try {
            String name = cmsCardLimitRepository.get().findCardholderNameByCardNumber(cardNumber);
            return name == null || name.isBlank() ? "Non renseigné" : name.trim();
        } catch (RuntimeException ex) {
            log.warn("Nom du titulaire indisponible depuis SVFE pour le rapport fraude : {}", ex.getMessage());
            return "Non renseigné";
        }
    }

    private String cardDigits(String source) {
        if (source == null) return null;
        String digits = source.replaceAll("\\D", "");
        return digits.matches("\\d{8,19}") ? digits : null;
    }

    private String maskPan(String source) {
        String digits = cardDigits(source);
        if (digits == null) return "Non renseigné";
        return digits.substring(0, 4) + "*".repeat(digits.length() - 8)
                + digits.substring(digits.length() - 4);
    }

    private long countFor(List<Map<String, Object>> values, String label) {
        return values.stream()
                .filter(value -> label.equalsIgnoreCase(String.valueOf(value.get("label"))))
                .map(value -> value.get("total"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .sum();
    }

    private MapSqlParameterSource parameters(LocalDateTime from, LocalDateTime until) {
        return new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("until", until);
    }

    private int number(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String normalizeFormat(String requestedFormat) {
        return requestedFormat == null || requestedFormat.isBlank()
                ? "WORD" : requestedFormat.trim().toUpperCase(Locale.ROOT);
    }

    private void validateFormat(String format) {
        if (!FORMATS.contains(format)) {
            throw new BusinessException("INVALID_FORMAT", "Format de rapport non pris en charge.");
        }
    }

    private Path documentPath(LocalDate date, String format) {
        String extension = switch (format) {
            case "PDF" -> ".pdf";
            case "EXCEL" -> ".xlsx";
            default -> ".docx";
        };
        return Path.of(outputDir).resolve("rapport_fraude_journalier_" + date + extension);
    }

    private FraudDailyReportDto toDto(FraudDailyReport report) {
        return FraudDailyReportDto.builder()
                .id(report.getId())
                .reportDate(report.getReportDate())
                .totalDetected(report.getTotalDetected())
                .totalProcessed(report.getTotalProcessed())
                .totalUnprocessed(report.getTotalUnprocessed())
                .generatedAt(report.getGeneratedAt())
                .status(report.getStatus())
                .errorMessage(report.getErrorMessage())
                .downloadable(FORMATS.stream().anyMatch(format -> Files.isRegularFile(documentPath(report.getReportDate(), format))))
                .availableFormats(FORMATS.stream().filter(format -> Files.isRegularFile(documentPath(report.getReportDate(), format))).toList())
                .build();
    }
}
