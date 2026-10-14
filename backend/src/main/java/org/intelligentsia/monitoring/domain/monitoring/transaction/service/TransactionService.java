package org.intelligentsia.monitoring.domain.monitoring.transaction.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.transaction.dto.TransactionErrorDto;
import org.intelligentsia.monitoring.domain.monitoring.transaction.dto.TransactionRateDto;
import org.intelligentsia.monitoring.domain.monitoring.transaction.entity.TransactionError;
import org.intelligentsia.monitoring.domain.monitoring.transaction.entity.TransactionRate;
import org.intelligentsia.monitoring.domain.monitoring.transaction.repository.TransactionErrorRepository;
import org.intelligentsia.monitoring.domain.monitoring.transaction.repository.TransactionRateRepository;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor
public class TransactionService {
    private final TransactionRateRepository rateRepository;
    private final TransactionErrorRepository errorRepository;
    private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<String> networks(Long institutionId) {
        if (institutionId != null) return List.of();
        return jdbc.queryForList("""
                SELECT network FROM transaction_rates WHERE network IS NOT NULL
                UNION SELECT network FROM transaction_errors WHERE network IS NOT NULL
                UNION SELECT network_code FROM network_snapshots WHERE network_code IS NOT NULL
                ORDER BY network
                """, java.util.Map.of(), String.class);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionRateDto> findRateHistory(Long institutionId, Instant from, Instant to, Pageable pageable, String network, String state) {
        if (institutionId != null) return PageResponse.of(Page.empty(pageable));
        return PageResponse.of(rateRepository.findFiltered(from, to, optional(network), optional(state), pageable).map(this::toRateDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionErrorDto> findErrorHistory(Long institutionId, Instant from, Instant to, Pageable pageable, String network, String state) {
        if (institutionId != null) return PageResponse.of(Page.empty(pageable));
        return PageResponse.of(errorRepository.findFiltered(from, to, optional(network), optional(state), pageable).map(this::toErrorDto));
    }

    @Transactional(readOnly = true)
    public List<String> transactionTypes(Long institutionId) {
        return jdbc.queryForList("""
            SELECT DISTINCT transaction_type FROM fraud_detections
            WHERE transaction_type IS NOT NULL AND trim(transaction_type) <> ''
              AND (CAST(:institutionId AS bigint) IS NULL OR institution_id=:institutionId)
            ORDER BY transaction_type
            """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("institutionId", institutionId), String.class);
    }

    /** Individual transaction details currently available from the detection source. */
    @Transactional(readOnly = true)
    public PageResponse<java.util.Map<String, Object>> operations(Long institutionId, java.time.LocalDateTime from,
            java.time.LocalDateTime until, String type, String search, Pageable pageable) {
        if (from != null && until != null && !from.isBefore(until))
            throw new org.intelligentsia.monitoring.infrastructure.exception.BusinessException("INVALID_PERIOD", "Le début doit précéder la fin.");
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("institutionId", institutionId).addValue("from", from).addValue("until", until)
            .addValue("type", optional(type)).addValue("search", optional(search))
            .addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        String conditions = """
            FROM fraud_detections d LEFT JOIN institutions i ON i.id=d.institution_id
            WHERE (CAST(:institutionId AS bigint) IS NULL OR d.institution_id=:institutionId)
              AND (CAST(:from AS timestamp) IS NULL OR d.detected_at>=:from)
              AND (CAST(:until AS timestamp) IS NULL OR d.detected_at<:until)
              AND (CAST(:type AS text) IS NULL OR d.transaction_type=:type)
              AND (CAST(:search AS text) IS NULL OR lower(coalesce(d.retrieval_reference,'')) LIKE lower('%' || :search || '%')
                   OR lower(coalesce(d.auth_code,'')) LIKE lower('%' || :search || '%')
                   OR lower(coalesce(d.merchant_name,'')) LIKE lower('%' || :search || '%'))
            """;
        Long total = jdbc.queryForObject("SELECT count(*) " + conditions, params, Long.class);
        var rows = jdbc.query("""
            SELECT d.id, d.detected_at, d.transaction_type, d.amount, d.currency, d.merchant_name,
                   d.retrieval_reference, d.authorization_response, d.status, d.risk_rule_code,
                   d.application_risk_level, i.name AS institution_name
            """ + conditions + " ORDER BY d.detected_at DESC, d.id DESC LIMIT :limit OFFSET :offset", params, (rs, index) -> {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                var date = rs.getTimestamp("detected_at");
                row.put("detectedAt", date == null ? null : date.toLocalDateTime());
                row.put("transactionType", rs.getString("transaction_type")); row.put("amount", rs.getBigDecimal("amount"));
                row.put("currency", rs.getString("currency")); row.put("merchant", rs.getString("merchant_name"));
                row.put("reference", rs.getString("retrieval_reference")); row.put("responseCode", rs.getString("authorization_response"));
                row.put("status", rs.getString("status")); row.put("ruleCode", rs.getString("risk_rule_code"));
                row.put("riskLevel", rs.getString("application_risk_level")); row.put("institution", rs.getString("institution_name"));
                return row;
            });
        return PageResponse.of(new org.springframework.data.domain.PageImpl<>(rows, pageable, total == null ? 0 : total));
    }

    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    @Transactional(readOnly = true)
    public List<TransactionRateDto> findLatestRates(Long institutionId) {
        // The snapshot source currently has no institution key; never return a cross-institution aggregate under a selected filter.
        if (institutionId != null) return List.of();
        return rateRepository.findAllLatest().stream().map(this::toRateDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionRateDto> findRateHistory(Long institutionId, Instant from, Instant to, Pageable pageable) {
        if (institutionId != null) return PageResponse.of(Page.empty(pageable));
        if (from != null || to != null) {
            return PageResponse.of(rateRepository.findHistoryBetween(from, to, pageable).map(this::toRateDto));
        }
        return PageResponse.of(rateRepository.findHistory(pageable).map(this::toRateDto));
    }

    @Transactional(readOnly = true)
    public List<TransactionErrorDto> findLatestErrors(Long institutionId) {
        if (institutionId != null) return List.of();
        return errorRepository.findAllLatest().stream().map(this::toErrorDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionErrorDto> findErrorHistory(Long institutionId, Instant from, Instant to, Pageable pageable) {
        if (institutionId != null) return PageResponse.of(Page.empty(pageable));
        if (from != null || to != null) {
            return PageResponse.of(errorRepository.findHistoryBetween(from, to, pageable).map(this::toErrorDto));
        }
        return PageResponse.of(errorRepository.findHistory(pageable).map(this::toErrorDto));
    }

    private TransactionRateDto toRateDto(TransactionRate s) {
        TransactionRateDto dto = new TransactionRateDto();
        dto.setId(s.getId()); dto.setNetwork(s.getNetwork()); dto.setAcquirer(s.getAcquirer()); dto.setIssuer(s.getIssuer());
        dto.setSuccessCount(s.getSuccessCount()); dto.setSuccessRate(s.getSuccessRate());
        dto.setFailureCount(s.getFailureCount()); dto.setFailureRate(s.getFailureRate());
        dto.setSnapshotTime(s.getSnapshotTime());
        return dto;
    }

    private TransactionErrorDto toErrorDto(TransactionError s) {
        TransactionErrorDto dto = new TransactionErrorDto();
        dto.setId(s.getId()); dto.setNetwork(s.getNetwork()); dto.setResponseCode(s.getResponseCode());
        dto.setResponseDesc(s.getResponseDesc()); dto.setCount(s.getCount()); dto.setSnapshotTime(s.getSnapshotTime());
        return dto;
    }
}
