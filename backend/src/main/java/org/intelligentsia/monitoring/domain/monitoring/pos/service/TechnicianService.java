package org.intelligentsia.monitoring.domain.monitoring.pos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.TechnicianCreateRequest;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.TechnicianDto;
import org.intelligentsia.monitoring.domain.monitoring.pos.entity.Technician;
import org.intelligentsia.monitoring.domain.monitoring.pos.repository.TechnicianRepository;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class TechnicianService {
    private final TechnicianRepository technicianRepository;
    private final InstitutionRepository institutionRepository;

    @Transactional(readOnly = true)
    public PageResponse<TechnicianDto> findAll(FilterRequest filter, Long institutionId) {
        return PageResponse.of(technicianRepository.findWithFilters(institutionId, filter.getSearch(), filter.toPageable()).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public TechnicianDto findById(Long id) {
        return toDto(technicianRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Technicien", id)));
    }

    @Transactional(readOnly = true)
    public List<TechnicianDto> findByInstitution(Long institutionId) {
        return technicianRepository.findByInstitutionId(institutionId).stream().map(this::toDto).toList();
    }

    @Transactional
    public TechnicianDto create(TechnicianCreateRequest req) {
        Institution inst = institutionRepository.findById(req.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", req.getInstitutionId()));
        Technician t = new Technician();
        t.setInstitution(inst); t.setName(req.getName());
        t.setPhone(req.getPhone()); t.setCards(req.getCards());
        return toDto(technicianRepository.save(t));
    }

    @Transactional
    public TechnicianDto update(Long id, TechnicianCreateRequest req) {
        Technician t = technicianRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Technicien", id));
        t.setName(req.getName()); t.setPhone(req.getPhone()); t.setCards(req.getCards());
        return toDto(technicianRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        if (!technicianRepository.existsById(id)) throw new ResourceNotFoundException("Technicien", id);
        technicianRepository.deleteById(id);
    }

    private TechnicianDto toDto(Technician t) {
        TechnicianDto dto = new TechnicianDto();
        dto.setId(t.getId()); dto.setName(t.getName()); dto.setPhone(t.getPhone()); dto.setCards(t.getCards());
        dto.setInstitutionId(t.getInstitution().getId()); dto.setInstitutionName(t.getInstitution().getName());
        dto.setCreatedAt(t.getCreatedAt()); dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}
