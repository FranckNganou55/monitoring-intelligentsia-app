package org.intelligentsia.monitoring.domain.monitoring.pos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.PosCreateRequest;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.PosTerminalDto;
import org.intelligentsia.monitoring.domain.monitoring.pos.entity.PosTerminal;
import org.intelligentsia.monitoring.domain.monitoring.pos.entity.Technician;
import org.intelligentsia.monitoring.domain.monitoring.pos.repository.PosRepository;
import org.intelligentsia.monitoring.domain.monitoring.pos.repository.TechnicianRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j @Service @RequiredArgsConstructor
public class PosService {
    private final PosRepository posRepository;
    private final InstitutionRepository institutionRepository;
    private final TechnicianRepository technicianRepository;

    @Transactional(readOnly = true)
    public PageResponse<PosTerminalDto> findAll(FilterRequest filter, Long institutionId) {
        return PageResponse.of(posRepository.findWithFilters(institutionId, filter.getSearch(), filter.toPageable()).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public PosTerminalDto findById(Long id) {
        return toDto(posRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Terminal POS", id)));
    }

    @Transactional
    public PosTerminalDto create(PosCreateRequest req) {
        if (posRepository.existsByTerminalId(req.getTerminalId()))
            throw new BusinessException("TERMINAL_DUPLICATE", "Ce terminal POS existe déjà.");
        Institution inst = institutionRepository.findById(req.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", req.getInstitutionId()));
        PosTerminal pos = new PosTerminal();
        pos.setInstitution(inst); pos.setTerminalId(req.getTerminalId());
        pos.setMerchant(req.getMerchant()); pos.setCity(req.getCity());
        pos.setDistrict(req.getDistrict()); pos.setExtraName(req.getExtraName());
        if (req.getTechnicianId() != null)
            pos.setTechnician(technicianRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Technicien", req.getTechnicianId())));
        return toDto(posRepository.save(pos));
    }

    @Transactional
    public PosTerminalDto update(Long id, PosCreateRequest req) {
        PosTerminal pos = posRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Terminal POS", id));
        pos.setMerchant(req.getMerchant()); pos.setCity(req.getCity());
        pos.setDistrict(req.getDistrict()); pos.setExtraName(req.getExtraName());
        if (req.getTechnicianId() != null)
            pos.setTechnician(technicianRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Technicien", req.getTechnicianId())));
        else pos.setTechnician(null);
        return toDto(posRepository.save(pos));
    }

    @Transactional
    public void delete(Long id) {
        if (!posRepository.existsById(id)) throw new ResourceNotFoundException("Terminal POS", id);
        posRepository.deleteById(id);
    }

    private PosTerminalDto toDto(PosTerminal p) {
        PosTerminalDto dto = new PosTerminalDto();
        dto.setId(p.getId()); dto.setTerminalId(p.getTerminalId());
        dto.setMerchant(p.getMerchant()); dto.setCity(p.getCity());
        dto.setDistrict(p.getDistrict()); dto.setExtraName(p.getExtraName());
        dto.setInstitutionId(p.getInstitution().getId()); dto.setInstitutionName(p.getInstitution().getName());
        if (p.getTechnician() != null) {
            dto.setTechnicianId(p.getTechnician().getId()); dto.setTechnicianName(p.getTechnician().getName());
        }
        dto.setCreatedAt(p.getCreatedAt()); dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
