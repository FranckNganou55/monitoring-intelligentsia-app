package org.intelligentsia.monitoring.domain.extraction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.intelligentsia.monitoring.domain.extraction.entity.OutputFormat;

import java.util.List;
import java.util.Map;

@Data
public class ExtractionConfigRequest {
    @NotNull  private Long institutionId;
    @NotNull  private Long templateId;
    @NotBlank private String name;
    private String description;
    private Map<String, String> paramValues;
    @NotNull  private OutputFormat outputFormat;
    private String cronExpression;
    private boolean active = true;
    private List<DestinataireRequest> destinataires;

    @Data
    public static class DestinataireRequest {
        private String email;
        private String nom;
        private boolean active = true;
    }
}
