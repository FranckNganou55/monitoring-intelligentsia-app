package org.intelligentsia.monitoring.domain.configuration.institution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.institution.dto.*;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.*;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.*;
import org.intelligentsia.monitoring.infrastructure.exception.*;
import org.intelligentsia.monitoring.shared.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class InstitutionService {
    private final InstitutionRepository institutionRepository;
    private final CurrencyRepository currencyRepository;
    private final CountryCodeRepository countryCodeRepository;

    @Transactional(readOnly = true)
    public PageResponse<InstitutionDto> findAll(FilterRequest filter) {
        return PageResponse.of(institutionRepository.findWithSearch(filter.getSearch(), filter.toPageable()).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public InstitutionDto findById(Long id) {
        return toDto(institutionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institution", id)));
    }

    @Transactional
    public InstitutionDto create(InstitutionCreateRequest req) {
        String code = req.getCode().trim();
        if (institutionRepository.existsByCode(code))
            throw new BusinessException("CODE_TAKEN", "Ce code institution est déjà utilisé.");
        Institution inst = new Institution();
        inst.setCode(code); inst.setName(code);
        inst.setSvfeId(req.getSvfeId() == null || req.getSvfeId().isBlank() ? code : req.getSvfeId().trim());
        inst.setHostPid(req.getHostPid());
        inst.setMailRecharge(req.getMailRecharge());
        return toDto(institutionRepository.save(inst));
    }

    @Transactional
    public InstitutionDto update(Long id, InstitutionCreateRequest req) {
        Institution inst = institutionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institution", id));
        String code = req.getCode().trim();
        if (institutionRepository.existsByCodeAndIdNot(code, id))
            throw new BusinessException("CODE_TAKEN", "Ce code institution est déjà utilisé.");
        inst.setCode(code);
        inst.setName(code);
        inst.setSvfeId(req.getSvfeId() == null || req.getSvfeId().isBlank() ? code : req.getSvfeId().trim());
        return toDto(institutionRepository.save(inst));
    }

    @Transactional
    public InstitutionDto updateAlertRouting(Long id, InstitutionAlertRoutingRequest req) {
        Institution inst = institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", id));
        String countryNumericCode = req.getCountryNumericCode() == null || req.getCountryNumericCode().isBlank()
                ? null : req.getCountryNumericCode().trim();
        if (countryNumericCode != null && !countryCodeRepository.existsById(countryNumericCode)) {
            throw new BusinessException("COUNTRY_CODE_UNKNOWN", "Le code numérique sélectionné n’existe pas.");
        }
        inst.setCountryNumericCode(countryNumericCode);
        inst.setAlertEmailTypique(normalizeOptionalEmail(req.getAlertEmailTypique()));
        inst.setAlertEmailMoinsCritique(normalizeOptionalEmail(req.getAlertEmailMoinsCritique()));
        inst.setAlertEmailMoyen(normalizeOptionalEmail(req.getAlertEmailMoyen()));
        inst.setAlertEmailCritique(normalizeOptionalEmail(req.getAlertEmailCritique()));
        return toDto(institutionRepository.save(inst));
    }

    private String normalizeOptionalEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim();
    }

    @Transactional
    public void delete(Long id) {
        if (!institutionRepository.existsById(id)) throw new ResourceNotFoundException("Institution", id);
        institutionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<InstitutionDto> findAllForSelect() {
        return institutionRepository.findAll().stream().map(this::toDto).toList();
    }

    private InstitutionDto toDto(Institution i) {
        InstitutionDto dto = new InstitutionDto();
        dto.setId(i.getId()); dto.setCode(i.getCode()); dto.setName(i.getName());
        dto.setSvfeId(i.getSvfeId()); dto.setHostPid(i.getHostPid());
        dto.setMailRecharge(i.getMailRecharge());
        dto.setCountryNumericCode(i.getCountryNumericCode());
        dto.setAlertEmailTypique(i.getAlertEmailTypique());
        dto.setAlertEmailMoinsCritique(i.getAlertEmailMoinsCritique());
        dto.setAlertEmailMoyen(i.getAlertEmailMoyen());
        dto.setAlertEmailCritique(i.getAlertEmailCritique());
        dto.setCreatedAt(i.getCreatedAt()); dto.setUpdatedAt(i.getUpdatedAt());
        dto.setCurrencies(i.getCurrencies().stream().map(ic -> {
            InstitutionDto.CurrencyThresholdDto ct = new InstitutionDto.CurrencyThresholdDto();
            ct.setCurrencyId(ic.getCurrency().getId()); ct.setIsoAlpha(ic.getCurrency().getIsoAlpha());
            ct.setLabel(ic.getCurrency().getLabel()); ct.setAtmThreshold(ic.getAtmThreshold());
            ct.setCardPpLimit(ic.getCardPpLimit()); return ct;
        }).toList());
        return dto;
    }
}
