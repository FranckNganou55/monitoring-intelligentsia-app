package org.intelligentsia.monitoring.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final List<FieldError> errors;
    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
