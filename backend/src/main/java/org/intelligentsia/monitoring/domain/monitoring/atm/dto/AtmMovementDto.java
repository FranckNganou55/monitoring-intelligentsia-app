package org.intelligentsia.monitoring.domain.monitoring.atm.dto;

import java.time.Instant;

public record AtmMovementDto(long id, Instant observedAt, String source,
                             String previousStatus, String status, String state, String message) {}
