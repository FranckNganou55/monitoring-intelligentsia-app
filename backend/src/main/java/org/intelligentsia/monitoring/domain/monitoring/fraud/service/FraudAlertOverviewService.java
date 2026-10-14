package org.intelligentsia.monitoring.domain.monitoring.fraud.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.fraud.dto.FraudAlertOverviewDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FraudAlertOverviewService {

    private static final String BASE_WHERE = """
            FROM monitoring_alerts a
            LEFT JOIN fraud_detections fd ON fd.id = a.fraud_detection_id
            WHERE (a.category IN ('FRAUD', 'ATM') OR a.atm_id IS NOT NULL OR EXISTS (
                    SELECT 1 FROM monitoring_rules rule_filter
                    WHERE rule_filter.id = a.rule_id
                      AND rule_filter.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X')
                  ))
              AND a.deleted_at IS NULL
              AND a.detected_at >= :from AND a.detected_at < :until
              AND (CAST(:institutionId AS BIGINT) IS NULL OR a.institution_id = :institutionId
                   OR EXISTS (SELECT 1 FROM atms atm_scope WHERE atm_scope.id = a.atm_id AND atm_scope.institution_id = :institutionId))
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public FraudAlertOverviewDto getOverview(int requestedPeriodHours, int requestedPage, int requestedSize,
                                             Long institutionId, String search, String riskLevel, String status) {
        int periodHours = Math.max(1, Math.min(requestedPeriodHours, 720));
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 100));
        LocalDateTime until = LocalDateTime.now();
        LocalDateTime from = until.minusHours(periodHours);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();

        MapSqlParameterSource base = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("until", until)
                .addValue("institutionId", institutionId);

        List<Map<String, Object>> risks = jdbc.queryForList("""
                SELECT COALESCE(fd.application_risk_level, a.risk_level) AS label, COUNT(*) AS total
                """ + BASE_WHERE + " GROUP BY COALESCE(fd.application_risk_level, a.risk_level)", base);
        int critical = count(risks, "CRITIQUE");
        int medium = count(risks, "MOYEN");
        int typical = count(risks, "TYPIQUE");
        int lessCritical = count(risks, "MOINS_CRITIQUE");

        Map<String, Object> statuses = jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE a.status = 'OPEN') AS open_count,
                       COUNT(DISTINCT fd.id) FILTER (WHERE fd.status = 'CONFIRMED') AS confirmed_count
                FROM monitoring_alerts a
                LEFT JOIN fraud_detections fd ON fd.id = a.fraud_detection_id
                WHERE (a.category IN ('FRAUD', 'ATM') OR a.atm_id IS NOT NULL OR EXISTS (
                        SELECT 1 FROM monitoring_rules rule_filter
                        WHERE rule_filter.id = a.rule_id
                          AND rule_filter.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X')
                      )) AND a.deleted_at IS NULL
                  AND a.detected_at >= :from AND a.detected_at < :until
                  AND (CAST(:institutionId AS BIGINT) IS NULL OR a.institution_id = :institutionId
                       OR EXISTS (SELECT 1 FROM atms atm_scope WHERE atm_scope.id = a.atm_id AND atm_scope.institution_id = :institutionId))
                """, base);

        // date_trunc expects its first argument to be a PostgreSQL text literal.
        // Binding it as a JDBC parameter can leave PostgreSQL unable to resolve
        // the function signature on some driver/server combinations.
        String bucketExpression = periodHours <= 24
                ? "date_trunc('hour', a.detected_at)"
                : "date_trunc('day', a.detected_at)";
        List<FraudAlertOverviewDto.Bucket> groupedBuckets = jdbc.query("""
                SELECT __BUCKET_EXPRESSION__ AS bucket_time,
                       COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE COALESCE(fd.application_risk_level, a.risk_level) = 'CRITIQUE') AS critical_count,
                       COUNT(*) FILTER (WHERE COALESCE(fd.application_risk_level, a.risk_level) = 'MOYEN') AS medium_count,
                       COUNT(*) FILTER (WHERE COALESCE(fd.application_risk_level, a.risk_level) = 'TYPIQUE') AS typical_count,
                       COUNT(*) FILTER (WHERE COALESCE(fd.application_risk_level, a.risk_level) = 'MOINS_CRITIQUE') AS less_critical_count
                """.replace("__BUCKET_EXPRESSION__", bucketExpression)
                + BASE_WHERE + " GROUP BY 1 ORDER BY 1", base, (rs, row) -> {
            Object value = rs.getObject("bucket_time");
            LocalDateTime time = value instanceof Timestamp timestamp
                    ? timestamp.toLocalDateTime()
                    : (LocalDateTime) value;
            return FraudAlertOverviewDto.Bucket.builder().time(time).count(rs.getInt("total"))
                    .criticalCount(rs.getInt("critical_count"))
                    .mediumCount(rs.getInt("medium_count"))
                    .typicalCount(rs.getInt("typical_count"))
                    .lessCriticalCount(rs.getInt("less_critical_count")).build();
        });
        var bucketsByTime = new HashMap<LocalDateTime, FraudAlertOverviewDto.Bucket>();
        groupedBuckets.forEach(point -> bucketsByTime.put(point.getTime(), point));
        ChronoUnit bucketUnit = periodHours <= 24 ? ChronoUnit.HOURS : ChronoUnit.DAYS;
        LocalDateTime firstBucket = bucketUnit == ChronoUnit.HOURS
                ? from.truncatedTo(ChronoUnit.HOURS) : from.toLocalDate().atStartOfDay();
        LocalDateTime finalBucket = bucketUnit == ChronoUnit.HOURS
                ? until.truncatedTo(ChronoUnit.HOURS) : until.toLocalDate().atStartOfDay();
        List<FraudAlertOverviewDto.Bucket> hourly = new ArrayList<>();
        for (LocalDateTime cursor = firstBucket; !cursor.isAfter(finalBucket); cursor = cursor.plus(1, bucketUnit)) {
            hourly.add(bucketsByTime.getOrDefault(cursor, FraudAlertOverviewDto.Bucket.builder()
                    .time(cursor).count(0).criticalCount(0).mediumCount(0).typicalCount(0).lessCriticalCount(0).build()));
        }

        String cardDigits = "regexp_replace(COALESCE(fd.account_reference, ''), '[^0-9]', '', 'g')";
        String binsSql = """
                SELECT substring(%s from 1 for 6) AS label, COUNT(*) AS total
                FROM monitoring_alerts a
                JOIN fraud_detections fd ON fd.id = a.fraud_detection_id
                WHERE (a.category IN ('FRAUD', 'ATM') OR a.atm_id IS NOT NULL OR EXISTS (
                        SELECT 1 FROM monitoring_rules rule_filter
                        WHERE rule_filter.id = a.rule_id
                          AND rule_filter.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X')
                      )) AND a.deleted_at IS NULL
                  AND a.detected_at >= :from AND a.detected_at < :until
                  AND (CAST(:institutionId AS BIGINT) IS NULL OR a.institution_id = :institutionId
                       OR EXISTS (SELECT 1 FROM atms atm_scope WHERE atm_scope.id = a.atm_id AND atm_scope.institution_id = :institutionId))
                  AND length(%s) >= 6
                GROUP BY label ORDER BY total DESC, label LIMIT 5
                """.formatted(cardDigits, cardDigits);
        List<FraudAlertOverviewDto.Breakdown> bins = jdbc.query(binsSql, base, (rs, row) -> FraudAlertOverviewDto.Breakdown.builder()
                .label(rs.getString("label")).count(rs.getLong("total")).build());

        List<FraudAlertOverviewDto.Breakdown> rules = jdbc.query("""
                SELECT r.code AS label, r.name AS description, COUNT(*) AS total
                FROM monitoring_alerts a
                JOIN monitoring_rules r ON r.id = a.rule_id
                WHERE (a.category IN ('FRAUD', 'ATM') OR a.atm_id IS NOT NULL OR EXISTS (
                        SELECT 1 FROM monitoring_rules rule_filter
                        WHERE rule_filter.id = a.rule_id
                          AND rule_filter.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X')
                      )) AND a.deleted_at IS NULL
                  AND a.detected_at >= :from AND a.detected_at < :until
                  AND (CAST(:institutionId AS BIGINT) IS NULL OR a.institution_id = :institutionId
                       OR EXISTS (SELECT 1 FROM atms atm_scope WHERE atm_scope.id = a.atm_id AND atm_scope.institution_id = :institutionId))
                GROUP BY r.id, r.code, r.name ORDER BY total DESC, r.code, r.id LIMIT 5
                """, base, (rs, row) -> FraudAlertOverviewDto.Breakdown.builder()
                .label(rs.getString("label")).description(rs.getString("description"))
                .count(rs.getLong("total")).build());

        MapSqlParameterSource filtered = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("until", until)
                .addValue("institutionId", institutionId)
                .addValue("search", normalizedSearch)
                .addValue("riskLevel", normalize(riskLevel))
                .addValue("status", normalize(status));
        String filters = """
                AND (CAST(:riskLevel AS VARCHAR) IS NULL OR COALESCE(fd.application_risk_level, a.risk_level) = :riskLevel)
                AND (CAST(:status AS VARCHAR) IS NULL OR a.status = :status)
                AND (CAST(:search AS VARCHAR) IS NULL OR CAST(:search AS VARCHAR) = ''
                     OR LOWER(a.title) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
                     OR LOWER(COALESCE(r.code, '')) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
                     OR LOWER(COALESCE(r.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
                     OR LOWER(COALESCE(a.transaction_reference, '')) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
                     OR LOWER(COALESCE(fd.account_reference, '')) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
                     OR LOWER(COALESCE(atm.terminal_id, '')) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
                     OR LOWER(COALESCE(i.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%')))
                """;
        String joins = """
                LEFT JOIN fraud_detections fd ON fd.id = a.fraud_detection_id
                LEFT JOIN monitoring_rules r ON r.id = a.rule_id
                LEFT JOIN atms atm ON atm.id = a.atm_id
                LEFT JOIN institutions i ON i.id = a.institution_id
                """;
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM monitoring_alerts a " + joins + """
                WHERE (a.category IN ('FRAUD', 'ATM') OR a.atm_id IS NOT NULL OR EXISTS (
                        SELECT 1 FROM monitoring_rules rule_filter
                        WHERE rule_filter.id = a.rule_id
                          AND rule_filter.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X')
                      )) AND a.deleted_at IS NULL
                  AND a.detected_at >= :from AND a.detected_at < :until
                  AND (CAST(:institutionId AS BIGINT) IS NULL OR a.institution_id = :institutionId
                       OR EXISTS (SELECT 1 FROM atms atm_scope WHERE atm_scope.id = a.atm_id AND atm_scope.institution_id = :institutionId))
                """ + filters, filtered, Long.class);
        filtered.addValue("limit", size).addValue("offset", (long) page * size);
        List<FraudAlertOverviewDto.AlertRow> alerts = jdbc.query("""
                SELECT a.id, a.detected_at, COALESCE(fd.application_risk_level, a.risk_level) AS effective_risk_level,
                       fd.account_reference AS card_candidate, fd.amount, fd.currency,
                       atm.terminal_id, a.institution_id, i.name AS institution_name,
                       r.id AS rule_id, r.code AS rule_code, r.name AS rule_name,
                       a.title, a.status,
                       COALESCE(a.transaction_reference, fd.retrieval_reference) AS transaction_reference
                FROM monitoring_alerts a
                """ + joins + """
                WHERE (a.category IN ('FRAUD', 'ATM') OR a.atm_id IS NOT NULL OR EXISTS (
                        SELECT 1 FROM monitoring_rules rule_filter
                        WHERE rule_filter.id = a.rule_id
                          AND rule_filter.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X')
                      )) AND a.deleted_at IS NULL
                  AND a.detected_at >= :from AND a.detected_at < :until
                  AND (CAST(:institutionId AS BIGINT) IS NULL OR a.institution_id = :institutionId
                       OR EXISTS (SELECT 1 FROM atms atm_scope WHERE atm_scope.id = a.atm_id AND atm_scope.institution_id = :institutionId))
                """ + filters + " ORDER BY a.detected_at DESC, a.id DESC LIMIT :limit OFFSET :offset",
                filtered, (rs, row) -> FraudAlertOverviewDto.AlertRow.builder()
                        .id(rs.getLong("id"))
                        .detectedAt(rs.getTimestamp("detected_at").toLocalDateTime())
                        .riskLevel(rs.getString("effective_risk_level"))
                        .cardNumber(maskCard(rs.getString("card_candidate")))
                        .amount(rs.getBigDecimal("amount"))
                        .currency(rs.getString("currency"))
                        .terminalId(rs.getString("terminal_id"))
                        .institutionId((Long) rs.getObject("institution_id"))
                        .institutionName(rs.getString("institution_name"))
                        .ruleId(rs.getLong("rule_id"))
                        .ruleCode(rs.getString("rule_code"))
                        .ruleName(rs.getString("rule_name"))
                        .title(rs.getString("title"))
                        .status(rs.getString("status"))
                        .transactionReference(rs.getString("transaction_reference"))
                        .build());

        return FraudAlertOverviewDto.builder()
                .criticalCount(critical)
                .mediumCount(medium)
                .typicalCount(typical)
                .lessCriticalCount(lessCritical)
                .lowCount(typical + lessCritical)
                .toQualifyCount(((Number) statuses.get("open_count")).intValue())
                .confirmedCount(((Number) statuses.get("confirmed_count")).intValue())
                .hourlyAlerts(hourly)
                .topBins(bins)
                .topRules(rules)
                .alerts(alerts)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(total == 0 ? 0 : (int) Math.ceil((double) total / size))
                .build();
    }

    private int count(List<Map<String, Object>> values, String label) {
        return values.stream()
                .filter(row -> label.equalsIgnoreCase(String.valueOf(row.get("label"))))
                .map(row -> row.get("total"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToInt(Number::intValue)
                .sum();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String maskCard(String source) {
        if (source == null) return "—";
        String digits = source.replaceAll("\\D", "");
        if (digits.length() < 8 || digits.length() > 19) return "—";
        return digits.substring(0, 4) + "*".repeat(digits.length() - 8) + digits.substring(digits.length() - 4);
    }
}
