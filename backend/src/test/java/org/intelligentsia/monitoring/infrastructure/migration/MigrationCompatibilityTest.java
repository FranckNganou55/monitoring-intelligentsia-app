package org.intelligentsia.monitoring.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Runs only on the local test DB, in disposable schemas, never in public. */
@EnabledIfSystemProperty(named = "monitoring.migrationAudit", matches = "true")
class MigrationCompatibilityTest {
    private static final String URL = "jdbc:postgresql://localhost:5432/monitoring_test";
    private static final String USER = "neo-monitoring";
    private static final String PASS = "monitoring123";

    private Flyway flyway(String schema, String target) {
        return Flyway.configure().dataSource(URL, USER, PASS).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target(target).load();
    }
    private void sql(Connection connection, String query) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(query); }
    }
    private String scalar(Connection connection, String query) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(query)) {
            assertTrue(rows.next()); return rows.getString(1);
        }
    }
    private void drop(String schema) throws SQLException {
        if (!schema.matches("codex_audit_[a-f0-9]{32}")) throw new IllegalArgumentException("Unsafe schema");
        try (var connection = DriverManager.getConnection(URL, USER, PASS)) { sql(connection, "DROP SCHEMA IF EXISTS " + schema + " CASCADE"); }
    }
    @Test
    void upgradesV12ToV30PreservingLegacyCoreAndDocumentsIntentionalChanges() throws Exception {
        String schema = "codex_audit_" + UUID.randomUUID().toString().replace("-", "");
        try {
            flyway(schema, "12").migrate();
            try (var connection = DriverManager.getConnection(URL, USER, PASS)) {
                sql(connection, "SET search_path TO " + schema);
                Map<String, String> base = new HashMap<>();
                try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                        "SELECT table_name||'.'||column_name, data_type||':'||COALESCE(character_maximum_length::text,'') FROM information_schema.columns WHERE table_schema='" + schema + "'")) {
                    while (rows.next()) base.put(rows.getString(1), rows.getString(2));
                }
                sql(connection, "INSERT INTO institutions(code,name,svfe_id) VALUES ('AUDIT','Legacy institution','43')");
                sql(connection, "INSERT INTO atms(institution_id,terminal_id,svfe_pid) SELECT id,'AUDIT-GAB',43001 FROM institutions WHERE code='AUDIT'");
                sql(connection, "INSERT INTO atm_snapshots(atm_id,snapshot_time,status_id,status_desc) SELECT id,'2026-01-01 12:00:00','0','DOWN' FROM atms WHERE terminal_id='AUDIT-GAB'");
                sql(connection, "INSERT INTO users(login,email,password_hash,first_name,last_name,scope,institution_id) SELECT 'audit-user','audit@example.invalid','audit-hash','Audit','User','GLOBAL',id FROM institutions WHERE code='AUDIT'");
                sql(connection, "INSERT INTO roles(code,name) VALUES ('AUDIT_CUSTOM','Audit custom role')");
                sql(connection, "INSERT INTO user_roles SELECT u.id,r.id FROM users u CROSS JOIN roles r WHERE u.login='audit-user' AND r.code IN ('AUDIT_CUSTOM','ROLE_OPERATOR','ROLE_MANAGER')");
                sql(connection, "UPDATE app_settings SET valeur='preserved.smtp.example' WHERE cle='mail.smtp.host'");
                flyway(schema, "27").migrate();
                sql(connection, "UPDATE institutions SET country_code='+225',alert_email='legacy@example.invalid' WHERE code='AUDIT'");
                flyway(schema, "30").migrate();
                flyway(schema, "30").validate();
                for (var column : base.entrySet()) {
                    String[] key = column.getKey().split("\\.");
                    assertEquals(column.getValue(), scalar(connection, "SELECT data_type||':'||COALESCE(character_maximum_length::text,'') FROM information_schema.columns WHERE table_schema='" + schema + "' AND table_name='" + key[0] + "' AND column_name='" + key[1] + "'"), column.getKey());
                }
                assertEquals("43", scalar(connection, "SELECT svfe_id FROM institutions WHERE code='AUDIT'"));
                assertEquals("43001", scalar(connection, "SELECT svfe_pid FROM atms WHERE terminal_id='AUDIT-GAB'"));
                assertEquals("DOWN", scalar(connection, "SELECT status_desc FROM atm_snapshots s JOIN atms a ON a.id=s.atm_id WHERE terminal_id='AUDIT-GAB'"));
                assertEquals("preserved.smtp.example", scalar(connection, "SELECT valeur FROM app_settings WHERE cle='mail.smtp.host'"));
                assertEquals("ROLE_MANAGER", scalar(connection, "SELECT r.code FROM user_roles ur JOIN users u ON u.id=ur.user_id JOIN roles r ON r.id=ur.role_id WHERE u.login='audit-user'"));
                assertEquals("1", scalar(connection, "SELECT count(*) FROM user_roles ur JOIN users u ON u.id=ur.user_id WHERE u.login='audit-user'"));
                assertNull(scalar(connection, "SELECT country_numeric_code FROM institutions WHERE code='AUDIT'"));
                assertEquals("legacy@example.invalid", scalar(connection, "SELECT alert_email FROM institutions WHERE code='AUDIT'"));
                assertEquals("0", scalar(connection, "SELECT count(*) FROM information_schema.columns WHERE table_schema='" + schema + "' AND table_name='institutions' AND column_name='country_code'"));
                assertEquals("2", scalar(connection, "SELECT count(*) FROM scheduler_tasks WHERE code IN ('ATM_QUERY','FRAUD_QUERY')"));
            }
        } finally { drop(schema); }
    }
    @Test
    void freshDatabaseMigratesThroughAllThirtyVersions() throws Exception {
        String schema = "codex_audit_" + UUID.randomUUID().toString().replace("-", "");
        try {
            var migration = flyway(schema, "30");
            migration.migrate(); migration.validate();
            assertEquals(30, Arrays.stream(migration.info().applied()).filter(info -> info.getVersion() != null).count());
        } finally { drop(schema); }
    }
}
