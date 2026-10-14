package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.Types;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in read-only PostgreSQL check; the observations are SQL VALUES, never inserts. */
@EnabledIfSystemProperty(named = "monitoring.dbTests", matches = "true")
class AtmMovementSqlTest {
    @Test
    void deduplicatesBeforeFilteringAndKeepsPreviousStatus() {
        var source = new DriverManagerDataSource("jdbc:postgresql://localhost:5432/monitoring_test",
                "neo-monitoring", System.getenv("DB_PASS") == null ? "monitoring123" : System.getenv("DB_PASS"));
        var jdbc = new NamedParameterJdbcTemplate(source);
        String sql = (String) ReflectionTestUtils.getField(AtmMovementService.class, "HISTORY");
        String fixture = """
                WITH observations(id, observed_at, source, timeline, status, state, message) AS (VALUES
                (1, '2026-10-01 10:00Z'::timestamptz, 'GAB', 'GAB', '1', 'ok', ''),
                (2, '2026-10-01 10:03Z'::timestamptz, 'GAB', 'GAB', '1', 'ok', ''),
                (3, '2026-10-01 10:06Z'::timestamptz, 'GAB', 'GAB', '0', 'error', ''),
                (4, '2026-10-01 10:09Z'::timestamptz, 'GAB', 'GAB', '0', 'error', ''),
                (5, '2026-10-01 10:12Z'::timestamptz, 'GAB', 'GAB', '3', 'error', ''),
                (6, '2026-10-01 10:15Z'::timestamptz, 'GAB', 'GAB', '4', 'ok', ''),
                (7, '2026-10-01 10:18Z'::timestamptz, 'GAB', 'GAB', '4', 'warning', '')
                """;
        sql = fixture + sql.substring(sql.indexOf("), normalized AS"));
        var parameters = new MapSqlParameterSource().addValue("from", null, Types.TIMESTAMP)
                .addValue("to", null, Types.TIMESTAMP).addValue("status", null, Types.VARCHAR);
        assertEquals(5, jdbc.queryForList(sql, parameters).size());
        parameters.addValue("status", "DOWN");
        var rows = jdbc.queryForList(sql, parameters);
        assertEquals(1, rows.size());
        assertEquals("UP", rows.get(0).get("previous_status"));
        parameters.addValue("from", java.sql.Timestamp.from(java.time.Instant.parse("2026-10-01T10:07:00Z")));
        assertTrue(jdbc.queryForList(sql, parameters).isEmpty());
    }
}
