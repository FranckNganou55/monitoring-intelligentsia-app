package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.CreateAlertRequest;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.MonitoringAlertDto;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.alert.service.MonitoringAlertService;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.Atm;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.AtmInterfaceMonitoringState;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmInterfaceMonitoringStateRepository;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmRepository;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.domain.monitoring.rule.repository.MonitoringRuleRepository;
import org.intelligentsia.monitoring.domain.configuration.scheduler.repository.SchedulerTaskRepository;
import org.intelligentsia.monitoring.domain.configuration.settings.service.AppSettingService;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.SvfeAtmInterfaceRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Collects SVFE GAB interface data and creates one alert per continuous outage. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtmInterfaceMonitorService {
    private static final String DOWN_RULE_CODE = "RULE_ATM_INTERFACE_DOWN_5X";
    private static final String SIGNOFF_RULE_CODE = "RULE_ATM_INTERFACE_SIGNOFF_5X";
    private static final long DEFAULT_SAMPLE_INTERVAL_SECONDS = 180;
    private static final int DEFAULT_REQUIRED_SAMPLES = 5;

    private static final List<String> INTERFACE_FIELDS = List.of(
            "INT_CODE", "INT_IDEN", "INT_LABE", "INT_STAT", "INT_LAST_UP_TIME",
            "INT_LAST_DOWN_TIME", "INT_LOG_REQU_IN", "INT_LOG_REQU_OUT", "INT_PID",
            "INT_PRIM_ADRE", "INT_PRIM_PORT", "INT_SECO_ADRE", "INT_SECO_PORT",
            "INT_NUMB_THRE", "INT_NUMB_PROC", "INT_FLAG_RECO", "INT_NUMB_RECO",
            "INT_DELA_CONN", "INT_TIME_OUT_ACQU", "INT_TIME_OUT_ISSU", "INT_STAR_SCRI",
            "INT_STOP_SCRI", "INT_TRAC_FILE", "INT_TRAC_LEVE", "INT_CURR_FILE_EXT",
            "INT_TRAC_REFR_METH", "INT_TRAC_FILE_PERI", "INT_TRAC_FILE_SIZE",
            "INT_DEST_STAT_ID", "INT_SOUR_STAT_ID", "INT_VOIX_DEBI", "INT_ITY_CODE",
            "INT_PING_FREQ", "INT_SUPE_PING_FLAG", "INT_AUTO_STAR", "INT_AUTO_STOP",
            "INT_STOP_DECI_FREQ", "INT_HSM_PROD_TYPE", "INT_HSM_VERI_TYPE", "INT_HKY_CODE",
            "INT_PIN_LENG", "INT_BAN_CODE"
    );

    private final ObjectProvider<SvfeAtmInterfaceRepository> sourceRepository;
    private final AtmInterfaceMonitoringStateRepository stateRepository;
    private final MonitoringRuleRepository ruleRepository;
    private final MonitoringAlertService alertService;
    private final AtmRepository atmRepository;
    private final SchedulerTaskRepository schedulerTaskRepository;
    private final AppSettingService appSettingService;
    private final DataSource dataSource;

    /** Run once from ATM_QUERY; its configured interval defines the sample cadence. */
    @Transactional("transactionManager")
    public void collectAndEvaluate() {
        SvfeAtmInterfaceRepository source = sourceRepository.getIfAvailable();
        if (source == null) {
            log.debug("SVFE désactivé — surveillance des interfaces GAB ignorée.");
            return;
        }

        List<Map<String, Object>> rows = source.findAtmInterfaces();
        if (rows.isEmpty()) {
            log.info("Aucune interface GAB (INT_BAN_CODE=20) retournée par SVFE.");
            return;
        }

        Instant observedAt = Instant.now();
        long sampleIntervalSeconds = schedulerTaskRepository.findByCode("ATM_QUERY")
                .map(task -> Math.max(1, task.getIntervalMs() / 1000))
                .orElse(DEFAULT_SAMPLE_INTERVAL_SECONDS);
        int requiredSamples = Math.max(1, Math.min(20, appSettingService.getInteger(
                "alerts.atm.interface.consecutive.samples", DEFAULT_REQUIRED_SAMPLES)));
        persistSnapshots(rows, observedAt);
        Map<String, Atm> atmsByTerminal = new HashMap<>();
        atmRepository.findAll().forEach(atm -> atmsByTerminal.putIfAbsent(
                atm.getTerminalId().toUpperCase(Locale.ROOT), atm));

        int processed = 0;
        for (Map<String, Object> row : rows) {
            String intCode = value(row, "INT_CODE");
            if (intCode == null || intCode.isBlank()) {
                log.warn("Interface GAB ignorée : INT_CODE absent.");
                continue;
            }
            Integer code = integerValue(row, "INT_STAT");
            var config = configurationFor(code, sampleIntervalSeconds, requiredSamples);
            evaluate(row, intCode, observedAt, atmsByTerminal, config[0], (int) config[1]);
            processed++;
        }
        log.info("Surveillance des interfaces GAB terminée — {} interface(s) évaluée(s).", processed);
    }

    private long[] configurationFor(Integer code, long fallbackInterval, int fallbackSamples) {
        String status = code != null && code == 3 ? "SIGNOFF" : "DOWN";
        String prefix = "alerts.atm.interface." + status;
        long interval = Math.max(60000, Math.min(900000, appSettingService.getInteger(prefix + ".interval.ms", (int) (fallbackInterval * 1000))));
        int samples = Math.max(1, Math.min(20, appSettingService.getInteger(prefix + ".consecutive.samples", fallbackSamples)));
        return new long[]{interval / 1000, samples};
    }

    private void evaluate(Map<String, Object> row, String intCode, Instant now, Map<String, Atm> atmsByTerminal,
                         long sampleIntervalSeconds, int requiredSamples) {
        AtmInterfaceMonitoringState state = stateRepository.findById(intCode)
                .orElseGet(() -> {
                    AtmInterfaceMonitoringState created = new AtmInterfaceMonitoringState();
                    created.setIntCode(intCode);
                    created.setConsecutiveSamples(0);
                    created.setAlertRaisedForOutage(false);
                    return created;
                });

        Integer code = integerValue(row, "INT_STAT");
        // Keep the source system's numeric contract explicit. STATUS_LABEL can
        // be absent or use a different vocabulary in the production schema.
        String label = code == null ? value(row, "STATUS_LABEL") : switch (code) {
            case 0 -> "DOWN";
            case 1 -> "UP";
            case 3 -> "SIGNOFF";
            case 4 -> "SIGNON";
            default -> firstNonBlank(value(row, "STATUS_LABEL"), "UNKNOWN");
        };
        state.setInterfaceIdentifier(value(row, "INT_IDEN"));
        state.setInterfaceLabel(value(row, "INT_LABE"));

        // A source row sampled too soon is stored in history but is not counted twice.
        if (state.getLastSampleAt() != null && java.util.Objects.equals(code, state.getCurrentStatusCode())
                && Duration.between(state.getLastSampleAt(), now).getSeconds() < sampleIntervalSeconds) {
            stateRepository.save(state);
            return;
        }

        boolean failure = code != null && (code == 0 || code == 3);
        if (failure) {
            Integer previousCode = state.getConsecutiveStatusCode();
            boolean sameRun = code.equals(previousCode);
            boolean gapTooLong = state.getLastSampleAt() != null
                    && Duration.between(state.getLastSampleAt(), now).getSeconds() > sampleIntervalSeconds * 2;
            int count = sameRun && !gapTooLong ? state.getConsecutiveSamples() + 1 : 1;
            if (count == 1) {
                state.setOutageStartedAt(now);
                state.setAlertRaisedForOutage(false);
                state.setActiveAlertId(null);
            }
            state.setConsecutiveStatusCode(code);
            state.setConsecutiveSamples(count);
        } else {
            state.setConsecutiveStatusCode(null);
            state.setConsecutiveSamples(0);
            state.setOutageStartedAt(null);
            // UP (1) and SIGNON (4) are the only confirmed operational statuses.
            if (code != null && (code == 1 || code == 4)) {
                state.setAlertRaisedForOutage(false);
                state.setActiveAlertId(null);
            }
        }

        state.setCurrentStatusCode(code);
        state.setCurrentStatusLabel(label);
        state.setLastSampleAt(now);
        stateRepository.save(state);

        if (failure && state.getConsecutiveSamples() >= requiredSamples
                && !Boolean.TRUE.equals(state.getAlertRaisedForOutage())) {
            createOutageAlert(row, state, code, label, now, atmsByTerminal, requiredSamples, sampleIntervalSeconds);
        }
    }

    private void createOutageAlert(Map<String, Object> row, AtmInterfaceMonitoringState state,
                                   Integer code, String label, Instant now, Map<String, Atm> atmsByTerminal,
                                   int requiredSamples, long sampleIntervalSeconds) {
        String ruleCode = code == 0 ? DOWN_RULE_CODE : SIGNOFF_RULE_CODE;
        Optional<MonitoringRule> rule = ruleRepository.findFirstByCodeOrderByIdAsc(ruleCode);
        if (rule.isEmpty() || !Boolean.TRUE.equals(rule.get().getEnabled())) {
            log.warn("La règle {} est absente ou désactivée ; aucun e-mail/alerte ne sera créé.", ruleCode);
            return;
        }

        Atm atm = findAtm(row, atmsByTerminal);
        String interfaceLabel = firstNonBlank(value(row, "INT_LABE"), value(row, "INT_IDEN"), state.getIntCode());
        CreateAlertRequest request = new CreateAlertRequest();
        request.setRuleId(rule.get().getId());
        request.setCategory(MonitoringAlert.Category.ATM);
        request.setTitle(limit("GAB " + label + " (" + code + ") — " + interfaceLabel, 200));
        request.setDescription(buildDescription(row, code, label, state, now, requiredSamples, sampleIntervalSeconds));
        request.setAtmId(atm != null ? atm.getId() : null);
        request.setInstitutionId(atm != null && atm.getInstitution() != null ? atm.getInstitution().getId() : null);

        MonitoringAlertDto created = alertService.create(request);
        state.setAlertRaisedForOutage(true);
        state.setActiveAlertId(created.getId());
        stateRepository.save(state);
        log.warn("Alerte GAB {} créée après {} relevés consécutifs du statut {}.",
                interfaceLabel, state.getConsecutiveSamples(), label);
    }

    private String buildDescription(Map<String, Object> row, Integer code, String label,
                                    AtmInterfaceMonitoringState state, Instant now,
                                    int requiredSamples, long sampleIntervalSeconds) {
        return "Statut d'interface GAB maintenu pendant " + requiredSamples + " relevés consécutifs espacés de "
                + sampleIntervalSeconds + " seconde(s).\n"
                + "Interface : " + firstNonBlank(value(row, "INT_LABE"), "—") + " (INT_CODE " + state.getIntCode() + ")\n"
                + "Identifiant : " + firstNonBlank(value(row, "INT_IDEN"), "—") + "\n"
                + "Statut : " + code + " — " + firstNonBlank(label, "inconnu") + "\n"
                + "Premier relevé de la série : " + state.getOutageStartedAt() + "\n"
                + "Dernier relevé : " + now + "\n"
                + "Liaison primaire : " + endpoint(value(row, "INT_PRIM_ADRE"), value(row, "INT_PRIM_PORT")) + "\n"
                + "Liaison secondaire : " + endpoint(value(row, "INT_SECO_ADRE"), value(row, "INT_SECO_PORT"));
    }

    private Atm findAtm(Map<String, Object> row, Map<String, Atm> atmsByTerminal) {
        String identifier = value(row, "INT_IDEN");
        Atm atm = identifier != null ? atmsByTerminal.get(identifier.toUpperCase(Locale.ROOT)) : null;
        if (atm != null) return atm;
        String label = value(row, "INT_LABE");
        if (label != null && label.toUpperCase(Locale.ROOT).startsWith("ATM_")) {
            return atmsByTerminal.get(label.substring(4).toUpperCase(Locale.ROOT));
        }
        return null;
    }

    private void persistSnapshots(List<Map<String, Object>> rows, Instant observedAt) {
        String columns = String.join(", ", INTERFACE_FIELDS.stream().map(AtmInterfaceMonitorService::columnName).toList());
        String marks = String.join(", ", java.util.Collections.nCopies(INTERFACE_FIELDS.size() + 2, "?"));
        String sql = "INSERT INTO atm_interface_snapshots (observed_at, " + columns + ", status_label) VALUES (" + marks + ")";
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<Object[]> batch = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            List<Object> values = new ArrayList<>(INTERFACE_FIELDS.size() + 2);
            values.add(Timestamp.from(observedAt));
            for (String field : INTERFACE_FIELDS) {
                values.add("INT_STAT".equals(field) ? integerValue(row, field) : value(row, field));
            }
            values.add(value(row, "STATUS_LABEL"));
            batch.add(values.toArray());
        }
        jdbc.batchUpdate(sql, batch);
    }

    private static String columnName(String sourceName) {
        return sourceName.toLowerCase(Locale.ROOT);
    }

    private static String value(Map<String, Object> row, String key) {
        Object raw = row.get(key);
        if (raw == null) raw = row.get(key.toLowerCase(Locale.ROOT));
        if (raw == null) return null;
        if (raw instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        return raw.toString();
    }

    private static Integer integerValue(Map<String, Object> row, String key) {
        String value = value(row, key);
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            log.warn("Valeur {} invalide pour {} : {}", key, key, value);
            return null;
        }
    }

    private static String endpoint(String address, String port) {
        if (address == null && port == null) return "—";
        return firstNonBlank(address, "?") + (port == null || port.isBlank() ? "" : ":" + port);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "—";
    }

    private static String limit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
