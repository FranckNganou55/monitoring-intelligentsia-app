package org.intelligentsia.monitoring.domain.extraction.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ExtractionTemplateDto {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String content;
    private List<ParameterDef> parameters;
    private String module;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
