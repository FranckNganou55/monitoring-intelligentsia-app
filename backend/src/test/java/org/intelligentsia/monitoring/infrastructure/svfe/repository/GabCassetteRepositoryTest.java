package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GabCassetteRepositoryTest {
    @Test void jdbcNullsStayNullInJsonAndDateStaysIso() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("CODE_GAB", Long.class)).thenReturn(1067780L);
        when(rs.getString("NUMERO_GAB")).thenReturn("00108401001");
        when(rs.getTimestamp("DERNIERE_MAJ")).thenReturn(Timestamp.valueOf("2026-04-11 11:58:01"));
        var row = GabCassetteRepository.ROW_MAPPER.mapRow(rs, 0);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var json = mapper.readTree(mapper.writeValueAsString(row.cassette()));
        assertTrue(json.has("billetsRestants")); assertTrue(json.get("billetsRestants").isNull());
        assertTrue(json.get("soldeCassette").isNull());
        assertEquals("2026-04-11T11:58:01", json.get("derniereMaj").asText());
        assertEquals("00108401001", row.numeroGab());
    }
    @Test void rejectsSqlInjectionInSchemaConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new GabCassetteRepository(mock(NamedParameterJdbcTemplate.class), "MXP;DROP TABLE ATM_TERMINAL"));
    }
    @Test @EnabledIfSystemProperty(named = "monitoring.dbTests", matches = "true")
    void nativeSqlPicksLatestForEachCassetteAndPreservesNulls() throws Exception {
        // Disposable temporary tables only; PostgreSQL checks the portable tuple/MAX query,
        // not Oracle connectivity or Oracle-specific driver behaviour.
        try (Connection connection = DriverManager.getConnection(System.getProperty("monitoring.testJdbcUrl", "jdbc:postgresql://localhost:5432/monitoring_test"),
                System.getProperty("monitoring.testDbUser", "neo-monitoring"), System.getProperty("monitoring.testDbPassword", "monitoring123"))) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TEMP TABLE ATM_TERMINAL (ATE_CODE BIGINT, ATE_NUM VARCHAR(50), ATE_LABE VARCHAR(100)) ON COMMIT DROP");
                statement.execute("CREATE TEMP TABLE ATM_BALANCE_DETALS (ABD_ATE_CODE BIGINT, ABD_CASS INTEGER, ABD_CUR_CODE INTEGER, ABD_VALUE NUMERIC, ABD_REMA_NOTE BIGINT, ABD_REJE_NOTE BIGINT, ABD_DISP_NOTE BIGINT, ABD_DATE TIMESTAMP) ON COMMIT DROP");
                statement.execute("INSERT INTO ATM_TERMINAL VALUES (1, '0001', 'GAB Un'), (2, '0002', 'GAB Deux')");
                statement.execute("INSERT INTO ATM_BALANCE_DETALS VALUES (1,1,28,100,244,9,47,'2025-06-21 08:44:35'), (1,1,28,100,163,6,31,'2026-04-11 11:58:01'), (1,2,28,100,NULL,NULL,NULL,'2025-06-18 14:36:21'), (2,1,28,100,0,0,0,'2026-04-10 10:00:00')");
                var jdbc = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));
                var repository = new GabCassetteRepository(jdbc, "");
                var rows = repository.findLatest(null);
                assertEquals(3, rows.size()); assertEquals(163L, rows.get(0).cassette().billetsRestants());
                assertEquals(16300, rows.get(0).cassette().soldeCassette().intValueExact());
                assertEquals("2026-04-11T11:58:01", rows.get(0).cassette().derniereMaj().toString());
                assertNull(rows.get(1).cassette().billetsRestants()); assertNull(rows.get(1).cassette().soldeCassette());
                assertEquals(0L, rows.get(2).cassette().billetsRestants());
                assertEquals(2, repository.findLatest(List.of(1L)).size());
                assertTrue(repository.findLatest(List.of()).isEmpty());
            } finally { connection.rollback(); }
        }
    }
}
