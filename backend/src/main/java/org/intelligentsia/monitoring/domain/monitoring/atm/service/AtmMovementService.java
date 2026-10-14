package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.AtmMovementDto;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmMovementService {
    private final AtmService atms;
    private final NamedParameterJdbcTemplate jdbc;

    // Determine transitions over the full timeline BEFORE applying date/status filters.
    // Each interface has its own timeline; terminal health and interface state remain distinct.
    private static final String HISTORY = """
            WITH observations AS (
              SELECT s.id, s.snapshot_time AS observed_at, 'GAB' AS source, 'GAB' AS timeline,
                     COALESCE(NULLIF(UPPER(TRIM(s.status_id)), ''), NULLIF(UPPER(TRIM(s.status_desc)), ''), 'INCONNU') AS status,
                     CASE
                       WHEN UPPER(COALESCE(s.status_id, '')) IN ('0','DOWN','OFFLINE','OUTOFSERVICE','OUT_OF_SERVICE') THEN 'error'
                       WHEN UPPER(COALESCE(s.status_id, '')) IN ('1','3','UP','SIGNOFF','SIGN_OFF','WARN','WARNING') THEN 'warning'
                       WHEN UPPER(COALESCE(s.status_id, '')) IN ('4','SIGNON','SIGN_ON','INSERVICE','IN_SERVICE','OK','ONLINE') THEN 'ok'
                       ELSE 'unknown' END AS state,
                     COALESCE(s.status_message, s.status_desc, '') AS message
              FROM atm_snapshots s WHERE s.atm_id = :atmId
              UNION ALL
              SELECT s.id, s.observed_at, 'Interface', 'Interface:' || COALESCE(s.int_code, ''),
                     CASE s.int_stat WHEN 0 THEN 'DOWN' WHEN 1 THEN 'UP' WHEN 3 THEN 'SIGNOFF'
                          WHEN 4 THEN 'SIGNON' ELSE COALESCE(s.status_label, s.int_stat::text, 'INCONNU') END,
                     CASE WHEN s.int_stat=0 THEN 'error' WHEN s.int_stat IN (1,3) THEN 'warning' WHEN s.int_stat=4 THEN 'ok' ELSE 'unknown' END,
                     COALESCE(s.int_labe, s.int_iden, '')
              FROM atm_interface_snapshots s
              JOIN atms a ON a.id = :atmId
              WHERE UPPER(s.int_iden) = UPPER(a.terminal_id)
                 OR UPPER(s.int_labe) = 'ATM_' || UPPER(a.terminal_id)
            ), normalized AS (
              SELECT *, CASE status WHEN '0' THEN 'DOWN' WHEN '1' THEN 'UP'
                 WHEN '3' THEN 'SIGNOFF' WHEN '4' THEN 'SIGNON' ELSE status END AS status_label
              FROM observations
            ), sequence AS (
              SELECT *, ROW_NUMBER() OVER w AS sequence_number,
                     LAG(status_label) OVER w AS previous_status,
                     LAG(state) OVER w AS previous_state
              FROM normalized WINDOW w AS (PARTITION BY timeline ORDER BY observed_at, id)
            ), movements AS (
              SELECT * FROM sequence
              WHERE sequence_number = 1 OR status_label IS DISTINCT FROM previous_status OR state IS DISTINCT FROM previous_state
            )
            SELECT id, observed_at, source, previous_status, status_label, state, message FROM movements
            WHERE (CAST(:from AS timestamptz) IS NULL OR observed_at >= :from)
              AND (CAST(:to AS timestamptz) IS NULL OR observed_at <= :to)
              AND (CAST(:status AS text) IS NULL OR status_label = :status OR state = :status)
            """;

    public PageResponse<AtmMovementDto> page(Long id, Long institutionId, String status, Instant from, Instant to, int page, int size) {
        var parameters = parameters(id, institutionId, status, from, to);
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + HISTORY + ") history", parameters, Long.class);
        parameters.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        return PageResponse.of(new PageImpl<>(query(HISTORY + " ORDER BY observed_at DESC, source, id DESC LIMIT :limit OFFSET :offset", parameters), pageable, total == null ? 0 : total));
    }

    public List<AtmMovementDto> all(Long id, Long institutionId, String status, Instant from, Instant to) {
        return query(HISTORY + " ORDER BY observed_at DESC, source, id DESC", parameters(id, institutionId, status, from, to));
    }

    private MapSqlParameterSource parameters(Long id, Long institutionId, String status, Instant from, Instant to) {
        atms.findById(id, institutionId); // Enforce the same institution boundary as the detail endpoint.
        if (from != null && to != null && from.isAfter(to)) throw new BusinessException("INVALID_PERIOD", "La date de début doit précéder la date de fin.");
        String normalized = status == null || status.isBlank() ? null : status.trim();
        if (normalized != null && !List.of("DOWN", "UP", "SIGNOFF", "SIGNON", "ok", "warning", "error", "unknown").contains(normalized))
            throw new BusinessException("INVALID_ATM_STATUS", "Statut GAB invalide.");
        return new MapSqlParameterSource("atmId", id).addValue("status", normalized, Types.VARCHAR)
                .addValue("from", from == null ? null : java.sql.Timestamp.from(from), Types.TIMESTAMP)
                .addValue("to", to == null ? null : java.sql.Timestamp.from(to), Types.TIMESTAMP);
    }

    private List<AtmMovementDto> query(String sql, MapSqlParameterSource parameters) {
        return jdbc.query(sql, parameters, (row, index) -> new AtmMovementDto(row.getLong("id"),
                row.getTimestamp("observed_at").toInstant(), row.getString("source"), row.getString("previous_status"),
                row.getString("status_label"), row.getString("state"), row.getString("message")));
    }
}
