package org.intelligentsia.monitoring.domain.extraction.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class ExtractionConfigDto {
    private Long id;
    private Long institutionId;
    private String institutionName;
    private Long templateId;
    private String templateName;
    private String templateType;
    private List<ParameterDef> templateParameters;
    private String name;
    private String description;
    private Map<String, String> paramValues;
    private String outputFormat;
    private String cronExpression;
    private boolean active;
    private List<DestinataireDto> destinataires;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class DestinataireDto {
        private Long id;
        private String email;
        private String nom;
        private boolean active;
    }
}
