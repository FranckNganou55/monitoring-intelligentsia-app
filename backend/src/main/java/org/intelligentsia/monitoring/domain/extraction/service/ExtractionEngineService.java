package org.intelligentsia.monitoring.domain.extraction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.configuration.settings.service.MailConfigService;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionJobDto;
import org.intelligentsia.monitoring.domain.extraction.dto.ParameterDef;
import org.intelligentsia.monitoring.domain.extraction.entity.*;
import org.intelligentsia.monitoring.domain.extraction.repository.ExtractionConfigRepository;
import org.intelligentsia.monitoring.domain.extraction.repository.ExtractionJobRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.intelligentsia.monitoring.shared.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.sql.ResultSetMetaData;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionEngineService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ExtractionConfigRepository configRepository;
    private final ExtractionJobRepository jobRepository;
    private final MailConfigService mailConfig;
    private final ObjectMapper objectMapper;

    @Autowired(required = false) @Qualifier("svfeJdbcTemplate")
    private NamedParameterJdbcTemplate svfeJdbcTemplate;

    @Value("${app.extraction.output-dir:${java.io.tmpdir}/extractions}")
    private String outputDir;

    @Value("${app.extraction.max-rows:500000}")
    private int maxRows;

    // ---- Public API ----

    @Transactional(readOnly = true)
    public PageResponse<ExtractionJobDto> findJobs(FilterRequest filter, Long configId, Long institutionId) {
        return PageResponse.of(
                jobRepository.findWithFilters(configId, institutionId, null, filter.toPageable())
                        .map(this::toJobDto));
    }

    @Transactional(readOnly = true)
    public ExtractionJobDto findJobById(Long id) {
        return toJobDto(jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionJob", id)));
    }

    @Transactional
    public ExtractionJobDto triggerManual(Long configId, Map<String, String> extraParams) {
        ExtractionConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionConfig", configId));

        User currentUser = SecurityUtils.getCurrentUser().orElse(null);
        ExtractionJob job = createJob(config, TriggerType.MANUEL, currentUser);
        ExtractionJob saved = jobRepository.save(job);

        runAsync(saved.getId(), extraParams);
        return toJobDto(saved);
    }

    public void triggerScheduled(Long configId) {
        ExtractionConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionConfig", configId));

        ExtractionJob job = createJob(config, TriggerType.CRON, null);
        ExtractionJob saved = jobRepository.save(job);
        executeJob(saved.getId(), Map.of());
    }

    @Transactional(readOnly = true)
    public Path getJobFile(Long jobId) {
        ExtractionJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionJob", jobId));
        if (job.getFilePath() == null || job.getStatus() != JobStatus.SUCCES) {
            throw new BusinessException("FILE_NOT_AVAILABLE", "Le fichier n'est pas disponible pour ce job.");
        }
        Path path = Path.of(job.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException("FILE_NOT_FOUND", "Le fichier généré est introuvable sur le serveur.");
        }
        return path;
    }

    // ---- Internal execution ----

    @Async("extractionExecutor")
    public void runAsync(Long jobId, Map<String, String> extraParams) {
        executeJob(jobId, extraParams);
    }

    private void executeJob(Long jobId, Map<String, String> extraParams) {
        // findByIdEager initialises all lazy associations within its own transaction so
        // config.getTemplate(), config.getDestinataires(), etc. are in-memory afterwards.
        ExtractionJob job = jobRepository.findByIdEager(jobId).orElseThrow();
        ExtractionConfig config = job.getConfig();

        job.setStatus(JobStatus.EN_COURS);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        try {
            Map<String, String> params = mergeParams(config.getParamValues(), extraParams);
            Path file = buildOutputPath(config, jobId);
            int rowCount = streamToFile(config.getTemplate(), params, config.getOutputFormat(), file);

            job.setStatus(JobStatus.SUCCES);
            job.setRowCount(rowCount);
            job.setFilePath(file.toString());
            job.setEndedAt(Instant.now());
            jobRepository.save(job);

            sendToDestinataires(config, file, rowCount);
            log.info("Job d'extraction #{} terminé: {} lignes, fichier {}", jobId, rowCount, file.getFileName());

        } catch (Exception e) {
            log.error("Job d'extraction #{} échoué: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.ECHEC);
            job.setErrorMessage(e.getMessage());
            job.setEndedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    // ---- Streaming core ----

    private int streamToFile(ExtractionTemplate template, Map<String, String> params,
                             OutputFormat format, Path outPath) throws IOException {

        List<ParameterDef> paramDefs = deserializeParams(template.getParameters());
        MapSqlParameterSource sqlParams = new MapSqlParameterSource();
        for (ParameterDef def : paramDefs) {
            String raw = params.getOrDefault(def.getName(), def.getDefaultValue());
            if (raw == null && def.isRequired()) {
                throw new BusinessException("MISSING_PARAM",
                        "Paramètre requis manquant: " + def.getName());
            }
            sqlParams.addValue(def.getName(), castParam(raw, def.getType()));
        }

        if (svfeJdbcTemplate == null) {
            throw new BusinessException("SVFE_DISABLED",
                    "La connexion SVFE est désactivée (app.svfe.enabled=false). Aucune extraction n'est possible.");
        }

        String sql = template.getContent().strip();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).stripTrailing();

        if (format == OutputFormat.EXCEL) {
            return streamToExcel(sql, sqlParams, outPath);
        } else {
            return streamToCsv(sql, sqlParams, outPath);
        }
    }

    private int streamToCsv(String sql, MapSqlParameterSource sqlParams, Path outPath) throws IOException {
        int[] rowCount = {0};
        int[] cols     = {-1};

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(outPath.toFile()), "UTF-8"))) {

            svfeJdbcTemplate.query(sql, sqlParams, (RowCallbackHandler) rs -> {
                try {
                    if (cols[0] < 0) {
                        ResultSetMetaData meta = rs.getMetaData();
                        cols[0] = meta.getColumnCount();
                        String[] headers = new String[cols[0]];
                        for (int i = 1; i <= cols[0]; i++) headers[i - 1] = meta.getColumnLabel(i);
                        writer.writeNext(headers);
                    }
                    if (rowCount[0] >= maxRows) {
                        throw new RuntimeException("EXTRACTION_LIMIT_EXCEEDED:" + maxRows);
                    }
                    String[] row = new String[cols[0]];
                    for (int i = 1; i <= cols[0]; i++) {
                        Object val = rs.getObject(i);
                        row[i - 1] = val != null ? val.toString() : "";
                    }
                    writer.writeNext(row);
                    rowCount[0]++;
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return rowCount[0];
    }

    private int streamToExcel(String sql, MapSqlParameterSource sqlParams, Path outPath) throws IOException {
        int[] rowIdx = {0};
        int[] cols   = {-1};

        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        try {
            Sheet sheet = wb.createSheet("Extraction");
            CellStyle headerStyle = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            svfeJdbcTemplate.query(sql, sqlParams, (RowCallbackHandler) rs -> {
                try {
                    if (cols[0] < 0) {
                        ResultSetMetaData meta = rs.getMetaData();
                        cols[0] = meta.getColumnCount();
                        Row hdrRow = sheet.createRow(rowIdx[0]++);
                        for (int i = 1; i <= cols[0]; i++) {
                            Cell cell = hdrRow.createCell(i - 1);
                            cell.setCellValue(meta.getColumnLabel(i));
                            cell.setCellStyle(headerStyle);
                        }
                    }
                    int dataRows = rowIdx[0] - 1;
                    if (dataRows >= maxRows) {
                        throw new RuntimeException("EXTRACTION_LIMIT_EXCEEDED:" + maxRows);
                    }
                    Row row = sheet.createRow(rowIdx[0]++);
                    for (int i = 1; i <= cols[0]; i++) {
                        Object val = rs.getObject(i);
                        row.createCell(i - 1).setCellValue(val != null ? val.toString() : "");
                    }
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                wb.write(fos);
            }
        } finally {
            wb.dispose();
        }

        return Math.max(0, rowIdx[0] - 1);
    }

    // ---- File path builder ----

    private Path buildOutputPath(ExtractionConfig config, Long jobId) throws IOException {
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);
        String timestamp = LocalDateTime.now().format(FILE_DATE);
        String baseName  = "extraction_" + jobId + "_" + timestamp;
        String ext       = config.getOutputFormat() == OutputFormat.EXCEL ? ".xlsx" : ".csv";
        return dir.resolve(baseName + ext);
    }

    // ---- Mail ----

    private void sendToDestinataires(ExtractionConfig config, Path file, int rowCount) {
        if (!mailConfig.isMailEnabled()) {
            log.debug("mail.enabled=false — aucun email envoyé pour le job #{}", config.getId());
            return;
        }
        List<ExtractionDestinataire> actifs = config.getDestinataires().stream()
                .filter(ExtractionDestinataire::isActive).toList();
        if (actifs.isEmpty()) return;

        JavaMailSender sender = mailConfig.buildSender();
        String subject = "Extraction : " + config.getName() + " (" + rowCount + " lignes)";
        for (ExtractionDestinataire dest : actifs) {
            try {
                MimeMessage msg = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(mailConfig.getFromAddress(), mailConfig.getFromName());
                helper.setTo(dest.getEmail());
                helper.setSubject(subject);
                helper.setText(
                        "<p>Bonjour " + (dest.getNom() != null ? dest.getNom() : "") + ",</p>"
                        + "<p>Veuillez trouver ci-joint le fichier d'extraction <b>" + config.getName() + "</b>.</p>"
                        + "<p>Nombre de lignes : " + rowCount + "</p>",
                        true);
                helper.addAttachment(file.getFileName().toString(), file.toFile());
                sender.send(msg);
                log.debug("Email extraction envoyé à {}", dest.getEmail());
            } catch (Exception e) {
                log.error("Erreur envoi email extraction à {}: {}", dest.getEmail(), e.getMessage());
            }
        }
    }

    // ---- Helpers ----

    private ExtractionJob createJob(ExtractionConfig config, TriggerType trigger, User user) {
        ExtractionJob job = new ExtractionJob();
        job.setConfig(config);
        job.setTriggerType(trigger);
        job.setStatus(JobStatus.EN_ATTENTE);
        job.setTriggeredBy(user);
        return job;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> mergeParams(String configJson, Map<String, String> extra) {
        Map<String, String> base = new HashMap<>();
        if (configJson != null && !configJson.isBlank()) {
            try {
                base.putAll(objectMapper.readValue(configJson, new TypeReference<Map<String, String>>() {}));
            } catch (Exception ignored) {}
        }
        if (extra != null) base.putAll(extra);
        return base;
    }

    private List<ParameterDef> deserializeParams(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Object castParam(String raw, String type) {
        if (raw == null) return null;
        return switch (type == null ? "STRING" : type.toUpperCase()) {
            case "INTEGER" -> Integer.parseInt(raw);
            case "LONG"    -> Long.parseLong(raw);
            case "DECIMAL" -> new java.math.BigDecimal(raw);
            case "BOOLEAN" -> Boolean.parseBoolean(raw);
            default        -> raw;
        };
    }

    private ExtractionJobDto toJobDto(ExtractionJob j) {
        ExtractionJobDto dto = new ExtractionJobDto();
        dto.setId(j.getId());
        dto.setConfigId(j.getConfig().getId());
        dto.setConfigName(j.getConfig().getName());
        dto.setInstitutionId(j.getConfig().getInstitution().getId());
        dto.setInstitutionName(j.getConfig().getInstitution().getName());
        dto.setTriggerType(j.getTriggerType().name());
        dto.setStatus(j.getStatus().name());
        dto.setStartedAt(j.getStartedAt());
        dto.setEndedAt(j.getEndedAt());
        if (j.getStartedAt() != null && j.getEndedAt() != null) {
            dto.setDurationMs(j.getEndedAt().toEpochMilli() - j.getStartedAt().toEpochMilli());
        }
        dto.setRowCount(j.getRowCount());
        dto.setDownloadable(j.getStatus() == JobStatus.SUCCES && j.getFilePath() != null);
        dto.setErrorMessage(j.getErrorMessage());
        dto.setTriggeredByLogin(j.getTriggeredBy() != null ? j.getTriggeredBy().getLogin() : null);
        dto.setCreatedAt(j.getCreatedAt());
        return dto;
    }
}
