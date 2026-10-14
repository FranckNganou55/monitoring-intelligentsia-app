package org.intelligentsia.monitoring.domain.extraction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.intelligentsia.monitoring.domain.extraction.entity.TemplateType;

import java.util.List;

@Data
public class ExtractionTemplateRequest {
    @NotBlank private String name;
    private String description;
    @NotNull  private TemplateType type;
    @NotBlank private String content;
    private List<ParameterDef> parameters;
    private String module;
    private boolean active = true;
}
