package org.intelligentsia.monitoring.domain.extraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionTemplateDto;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionTemplateRequest;
import org.intelligentsia.monitoring.domain.extraction.dto.ParameterDef;
import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionTemplate;
import org.intelligentsia.monitoring.domain.extraction.entity.TemplateType;
import org.intelligentsia.monitoring.domain.extraction.repository.ExtractionTemplateRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionTemplateService {

    private static final Pattern FORBIDDEN_SQL = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|GRANT|REVOKE|EXEC|EXECUTE|CALL)\\b",
            Pattern.CASE_INSENSITIVE);

    private final ExtractionTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<ExtractionTemplateDto> findAll(FilterRequest filter) {
        return PageResponse.of(
                templateRepository.findWithFilters(filter.getSearch(), filter.toPageable())
                        .map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ExtractionTemplateDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ExtractionTemplateDto> findAllActive() {
        return templateRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public ExtractionTemplateDto create(ExtractionTemplateRequest req) {
        validateSqlContent(req.getType(), req.getContent());
        ExtractionTemplate t = new ExtractionTemplate();
        applyRequest(t, req);
        return toDto(templateRepository.save(t));
    }

    @Transactional
    public ExtractionTemplateDto update(Long id, ExtractionTemplateRequest req) {
        validateSqlContent(req.getType(), req.getContent());
        ExtractionTemplate t = getOrThrow(id);
        applyRequest(t, req);
        return toDto(templateRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) throw new ResourceNotFoundException("ExtractionTemplate", id);
        templateRepository.deleteById(id);
    }

    // --- helpers ---

    private void validateSqlContent(TemplateType type, String content) {
        if (type == TemplateType.SQL_QUERY && FORBIDDEN_SQL.matcher(content).find()) {
            throw new BusinessException("INVALID_SQL",
                    "La requête SQL ne doit contenir que des instructions SELECT.");
        }
    }

    private void applyRequest(ExtractionTemplate t, ExtractionTemplateRequest req) {
        t.setName(req.getName());
        t.setDescription(req.getDescription());
        t.setType(req.getType());
        t.setContent(req.getContent());
        t.setModule(req.getModule());
        t.setActive(req.isActive());
        t.setParameters(serializeParams(req.getParameters()));
    }

    private String serializeParams(List<ParameterDef> params) {
        if (params == null || params.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new BusinessException("SERIALIZATION_ERROR", "Erreur de sérialisation des paramètres.");
        }
    }

    private List<ParameterDef> deserializeParams(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Impossible de désérialiser les paramètres du template: {}", e.getMessage());
            return List.of();
        }
    }

    private ExtractionTemplate getOrThrow(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionTemplate", id));
    }

    ExtractionTemplateDto toDto(ExtractionTemplate t) {
        ExtractionTemplateDto dto = new ExtractionTemplateDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setDescription(t.getDescription());
        dto.setType(t.getType().name());
        dto.setContent(t.getContent());
        dto.setParameters(deserializeParams(t.getParameters()));
        dto.setModule(t.getModule());
        dto.setActive(t.isActive());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}
