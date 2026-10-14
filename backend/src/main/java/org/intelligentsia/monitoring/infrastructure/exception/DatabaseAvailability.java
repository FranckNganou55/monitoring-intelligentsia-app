package org.intelligentsia.monitoring.infrastructure.exception;

public final class DatabaseAvailability {
    private DatabaseAvailability() {}
    public static boolean unavailable(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.springframework.dao.DataAccessResourceFailureException
                || cause instanceof org.hibernate.exception.JDBCConnectionException) return true;
            if (cause instanceof java.sql.SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) return true;
            if (cause == cause.getCause()) break;
        }
        return false;
    }
    public static org.springframework.http.ResponseEntity<ErrorResponse> response() {
        return org.springframework.http.ResponseEntity.status(503).body(ErrorResponse.builder()
            .code("DATABASE_UNAVAILABLE").message("La base de données est indisponible. Aucune donnée ne peut être affichée.").build());
    }
}
