package org.intelligentsia.monitoring.infrastructure.report.generator;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import java.util.Map;

/** Display conventions shared by PDF, Word and Excel exports. */
public final class ReportDisplay {
    private ReportDisplay() {}
    public static String value(Object value) {
        if (value == null || value.toString().isBlank()) return "—";
        if (value instanceof Timestamp timestamp) return value(timestamp.toLocalDateTime());
        if (value instanceof LocalDateTime date) return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        if (value instanceof LocalDate date) return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return switch (value.toString()) {
            case "OPEN" -> "Ouverte";
            case "ACKNOWLEDGED" -> "Prise en compte";
            case "IN_PROGRESS" -> "En cours";
            case "RESOLVED" -> "Résolue";
            case "ARCHIVED" -> "Archivée";
            case "FRAUD" -> "Fraude";
            case "ATM" -> "GAB";
            case "NETWORK" -> "Réseau";
            case "MOINS_CRITIQUE", "MOINS CRITIQUE" -> "Moins critique";
            case "CRITIQUE" -> "Critique";
            case "MOYEN" -> "Moyen";
            case "TYPIQUE" -> "Typique";
            default -> value.toString();
        };
    }
    public static String recordTitle(Map<String, Object> row, int index) {
        Object id = row.getOrDefault("id", row.get("alert_id"));
        return id == null ? "Enregistrement " + index : "Alerte #" + id;
    }
    public static String riskColor(Map<String, Object> row) {
        String risk = String.valueOf(row.getOrDefault("risk", row.get("risk_level")));
        return switch (risk) {
            case "CRITIQUE", "Critique" -> "B42318";
            case "MOYEN", "Moyen" -> "B54708";
            case "MOINS_CRITIQUE", "MOINS CRITIQUE", "Moins critique" -> "947600";
            default -> "235B8E";
        };
    }
}
