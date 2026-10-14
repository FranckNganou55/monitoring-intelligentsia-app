package org.intelligentsia.monitoring.domain.extraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionConfigDto;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionConfigRequest;
import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionConfig;
import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionDestinataire;
import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionTemplate;
import org.intelligentsia.monitoring.domain.extraction.repository.ExtractionConfigRepository;
import org.intelligentsia.monitoring.domain.extraction.repository.ExtractionTemplateRepository;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.infrastructure.tenant.TenantContext;
import org.intelligentsia.monitoring.shared.annotation.TenantAware;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionConfigService {

    private final ExtractionConfigRepository configRepository;
    private final ExtractionTemplateRepository templateRepository;
    private final InstitutionRepository institutionRepository;
    private final ExtractionTemplateService templateService;
    private final ObjectMapper objectMapper;

    @TenantAware
    @Transactional(readOnly = true)
    public PageResponse<ExtractionConfigDto> findAll(FilterRequest filter, Long selectedInstitutionId) {
        Long institutionId = TenantContext.get() != null ? TenantContext.get() : selectedInstitutionId;
        return PageResponse.of(
                configRepository.findWithFilters(institutionId, filter.getSearch(), filter.toPageable())
                        .map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ExtractionConfigDto findById(Long id, Long institutionId) {
        ExtractionConfigDto dto = toDto(getOrThrow(id));
        if (institutionId != null && !institutionId.equals(dto.getInstitutionId())) {
            throw new ResourceNotFoundException("ExtractionConfig", id);
        }
        return dto;
    }

    @Transactional
    public ExtractionConfigDto create(ExtractionConfigRequest req) {
        Institution institution = institutionRepository.findById(req.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", req.getInstitutionId()));
        ExtractionTemplate template = templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionTemplate", req.getTemplateId()));

        ExtractionConfig config = new ExtractionConfig();
        config.setInstitution(institution);
        config.setTemplate(template);
        applyRequest(config, req);

        return toDto(configRepository.save(config));
    }

    @Transactional
    public ExtractionConfigDto update(Long id, ExtractionConfigRequest req) {
        ExtractionConfig config = getOrThrow(id);

        if (!config.getTemplate().getId().equals(req.getTemplateId())) {
            ExtractionTemplate template = templateRepository.findById(req.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("ExtractionTemplate", req.getTemplateId()));
            config.setTemplate(template);
        }
        if (!config.getInstitution().getId().equals(req.getInstitutionId())) {
            Institution institution = institutionRepository.findById(req.getInstitutionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Institution", req.getInstitutionId()));
            config.setInstitution(institution);
        }

        applyRequest(config, req);
        return toDto(configRepository.save(config));
    }

    @Transactional
    public void delete(Long id) {
        if (!configRepository.existsById(id)) throw new ResourceNotFoundException("ExtractionConfig", id);
        configRepository.deleteById(id);
    }

    // --- helpers ---

    private void applyRequest(ExtractionConfig config, ExtractionConfigRequest req) {
        config.setName(req.getName());
        config.setDescription(req.getDescription());
        config.setOutputFormat(req.getOutputFormat());
        config.setCronExpression(req.getCronExpression());
        config.setActive(req.isActive());
        config.setParamValues(serializeMap(req.getParamValues()));

        config.getDestinataires().clear();
        if (req.getDestinataires() != null) {
            req.getDestinataires().forEach(dr -> {
                ExtractionDestinataire d = new ExtractionDestinataire();
                d.setEmail(dr.getEmail());
                d.setNom(dr.getNom());
                d.setActive(dr.isActive());
                d.setConfig(config);
                config.getDestinataires().add(d);
            });
        }
    }

    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, String> deserializeMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private ExtractionConfig getOrThrow(Long id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExtractionConfig", id));
    }

    private ExtractionConfigDto toDto(ExtractionConfig c) {
        ExtractionConfigDto dto = new ExtractionConfigDto();
        dto.setId(c.getId());
        dto.setInstitutionId(c.getInstitution().getId());
        dto.setInstitutionName(c.getInstitution().getName());
        dto.setTemplateId(c.getTemplate().getId());
        dto.setTemplateName(c.getTemplate().getName());
        dto.setTemplateType(c.getTemplate().getType().name());
        dto.setTemplateParameters(templateService.toDto(c.getTemplate()).getParameters());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setParamValues(deserializeMap(c.getParamValues()));
        dto.setOutputFormat(c.getOutputFormat().name());
        dto.setCronExpression(c.getCronExpression());
        dto.setActive(c.isActive());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        dto.setDestinataires(c.getDestinataires().stream().map(d -> {
            ExtractionConfigDto.DestinataireDto dd = new ExtractionConfigDto.DestinataireDto();
            dd.setId(d.getId());
            dd.setEmail(d.getEmail());
            dd.setNom(d.getNom());
            dd.setActive(d.isActive());
            return dd;
        }).toList());
        return dto;
    }
}
