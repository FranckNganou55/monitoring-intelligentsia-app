package org.intelligentsia.monitoring.infrastructure.local;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Authoritative application configuration. Business data and sessions are never written to this file. */
@Component
@ConditionalOnProperty(name="app.local-configuration.enabled", havingValue="true")
public class LocalApplicationState {
    static final List<String> TABLES = List.of("permissions", "roles", "role_permissions", "users", "user_roles",
        "password_history", "app_settings", "monitoring_rules", "scheduler_tasks", "extraction_templates", "extraction_configs");
    private final JdbcTemplate jdbc;
    private final Path file;
    private final ObjectMapper json = new ObjectMapper();
    private boolean loaded;

    public LocalApplicationState(JdbcTemplate jdbc, @Value("${app.local-configuration.file}") String file) {
        this.jdbc=jdbc; this.file=Path.of(file).toAbsolutePath().normalize();
    }

    public synchronized void load() {
        if (loaded) return;
        if (!Files.isRegularFile(file)) throw new IllegalStateException("Le fichier de configuration locale est absent : " + file);
        try {
            JsonNode snapshot=json.readTree(file.toFile());
            jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
            try {
                for (String table: TABLES) {
                    Set<String> columns=columns(table);
                    for (JsonNode row: snapshot.path(table)) {
                        List<String> names=new ArrayList<>(); List<Object> values=new ArrayList<>();
                        row.fields().forEachRemaining(entry -> {
                            if (columns.contains(entry.getKey())) {
                                names.add(entry.getKey());
                                JsonNode value=entry.getValue();
                                values.add(value.isNull() ? null : value.isObject() || value.isArray() ? value.toString() : json.convertValue(value,Object.class));
                            }
                        });
                        if (!names.isEmpty()) jdbc.update("INSERT INTO " + table + " (" + String.join(",",names) + ") VALUES (" + String.join(",", Collections.nCopies(names.size(),"?")) + ")", values.toArray());
                    }
                    if (columns.contains("id")) {
                        Long next=jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM " + table,Long.class);
                        jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + next);
                    }
                }
            } finally { jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE"); }
            loaded=true;
        } catch (Exception e) { throw new IllegalStateException("Impossible de charger la configuration locale.",e); }
    }

    private Set<String> columns(String table) {
        return new HashSet<>(jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name=?",String.class,table));
    }

    public synchronized void save() {
        if (!loaded) return;
        try {
            ObjectNode snapshot=json.createObjectNode();
            for (String table: TABLES) {
                ArrayNode rows=snapshot.putArray(table);
                for (Map<String,Object> record: jdbc.queryForList("SELECT * FROM " + table)) {
                    ObjectNode row=rows.addObject();
                    for (var entry:record.entrySet()) {
                        Object value=entry.getValue();
                        // JDBC timestamps and dates must survive a JVM restart, without serialization modules.
                        if (value instanceof java.util.Date || value instanceof java.time.temporal.TemporalAccessor) value=value.toString();
                        if (value instanceof byte[] bytes) value=json.readTree(bytes);
                        row.set(entry.getKey(),json.valueToTree(value));
                    }
                    // Institution rows are business data; keep only the external reference as configuration.
                    if (table.equals("users") || table.equals("extraction_configs")) row.putNull("institution_id");
                }
            }
            Path temp=file.resolveSibling(file.getFileName()+".tmp");
            Files.writeString(temp,json.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot));
            try { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception e) { throw new IllegalStateException("Impossible d’enregistrer la configuration locale.",e); }
    }
}
