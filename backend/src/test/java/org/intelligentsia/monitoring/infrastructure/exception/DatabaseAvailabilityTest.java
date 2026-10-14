package org.intelligentsia.monitoring.infrastructure.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseAvailabilityTest {
    @Test void distinguishesConnectionFailureFromQueryBugs() {
        var error = new org.springframework.transaction.CannotCreateTransactionException("Connection failed",
            new java.sql.SQLException("Connection refused", "08001"));
        assertTrue(DatabaseAvailability.unavailable(error));
        var response = new GlobalExceptionHandler().handleGeneric(error);
        assertEquals(503, response.getStatusCode().value());
        assertEquals("DATABASE_UNAVAILABLE", response.getBody().getCode());
        assertFalse(DatabaseAvailability.unavailable(new java.sql.SQLException("Invalid SQL", "42601")));
        assertFalse(DatabaseAvailability.unavailable(new IllegalArgumentException("invalid input")));
    }
}
