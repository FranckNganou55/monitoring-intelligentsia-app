package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Currency;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.CurrencyRepository;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.*;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.*;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.*;
import org.intelligentsia.monitoring.infrastructure.exception.*;
import org.intelligentsia.monitoring.shared.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class AtmService {
    private final AtmRepository atmRepository;
    private final AtmSnapshotRepository snapshotRepository;
    private final InstitutionRepository institutionRepository;
    private final CurrencyRepository currencyRepository;
    private final org.springframework.jdbc.core.JdbcTemplate statusJdbc;

    @Transactional(readOnly = true)
    public PageResponse<AtmDto> findAll(FilterRequest filter, Long institutionId, String status) {
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toLowerCase();
        if (normalizedStatus != null && !List.of("ok", "warning", "error", "unknown").contains(normalizedStatus)) {
            throw new BusinessException("INVALID_ATM_STATUS", "Statut GAB invalide.");
        }
        return PageResponse.of(atmRepository.findWithFilters(institutionId, filter.getSearch(), normalizedStatus, filter.toPageable()).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<AtmDto> findAllByInstitution(Long institutionId) {
        return atmRepository.findAllByInstitution(institutionId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AtmDto findById(Long id, Long institutionId) {
        Atm atm = findScopedAtm(id, institutionId);
        return toDto(atm);
    }

    @Transactional(readOnly = true)
    public AtmSnapshotDto findLatestSnapshot(Long id, Long institutionId) {
        Atm atm = findScopedAtm(id, institutionId);
        AtmSnapshotDto dto = snapshotRepository.findFirstByAtm_IdOrderBySnapshotTimeDesc(id)
                .map(this::toSnapshotDto).orElseGet(AtmSnapshotDto::new);
        dto.setAtmId(id); dto.setTerminalId(atm.getTerminalId());
        statusJdbc.query("SELECT * FROM atm_display_status WHERE atm_id=?", (org.springframework.jdbc.core.RowCallbackHandler) row -> {
            dto.setStatusId("unknown".equals(row.getString("status_key")) ? "UNKNOWN" : row.getString("status_id")); dto.setStatusDesc(row.getString("status_desc"));
            if (row.getTimestamp("observed_at") != null) dto.setSnapshotTime(row.getTimestamp("observed_at").toInstant());
        }, id);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<AtmSnapshotDto> findSnapshots(Long id, Long institutionId, int limit) {
        findScopedAtm(id, institutionId);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return snapshotRepository.findByAtmIdOrderByTimeDesc(
                        id, PageRequest.of(0, safeLimit))
                .map(this::toSnapshotDto).getContent();
    }

    private Atm findScopedAtm(Long id, Long institutionId) {
        Atm atm = atmRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ATM", id));
        if (institutionId != null && !institutionId.equals(atm.getInstitution().getId())) {
            throw new ResourceNotFoundException("ATM", id);
        }
        return atm;
    }

    @Transactional
    public AtmDto create(AtmCreateRequest req) {
        if (atmRepository.existsByInstitutionIdAndTerminalId(req.getInstitutionId(), req.getTerminalId()))
            throw new BusinessException("TERMINAL_DUPLICATE", "Ce terminal existe déjà pour cette institution.");
        Institution inst = institutionRepository.findById(req.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", req.getInstitutionId()));
        Atm atm = new Atm();
        atm.setInstitution(inst); atm.setTerminalId(req.getTerminalId());
        atm.setAddress(req.getAddress()); atm.setCity(req.getCity());
        atm.setSvfePid(req.getSvfePid()); atm.setThreshold1(req.getThreshold1()); atm.setThreshold2(req.getThreshold2());
        if (req.getCurrency1Id() != null)
            atm.setCurrency1(currencyRepository.findById(req.getCurrency1Id())
                    .orElseThrow(() -> new ResourceNotFoundException("Devise", req.getCurrency1Id())));
        if (req.getCurrency2Id() != null)
            atm.setCurrency2(currencyRepository.findById(req.getCurrency2Id())
                    .orElseThrow(() -> new ResourceNotFoundException("Devise", req.getCurrency2Id())));
        return toDto(atmRepository.save(atm));
    }

    @Transactional
    public AtmDto update(Long id, AtmCreateRequest req) {
        Atm atm = atmRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ATM", id));
        atm.setAddress(req.getAddress()); atm.setCity(req.getCity());
        atm.setThreshold1(req.getThreshold1()); atm.setThreshold2(req.getThreshold2());
        if (req.getCurrency1Id() != null)
            atm.setCurrency1(currencyRepository.findById(req.getCurrency1Id())
                    .orElseThrow(() -> new ResourceNotFoundException("Devise", req.getCurrency1Id())));
        if (req.getCurrency2Id() != null)
            atm.setCurrency2(currencyRepository.findById(req.getCurrency2Id())
                    .orElseThrow(() -> new ResourceNotFoundException("Devise", req.getCurrency2Id())));
        return toDto(atmRepository.save(atm));
    }

    @Transactional
    public void delete(Long id) {
        if (!atmRepository.existsById(id)) throw new ResourceNotFoundException("ATM", id);
        atmRepository.deleteById(id);
    }

    private AtmDto toDto(Atm a) {
        AtmDto dto = new AtmDto();
        dto.setId(a.getId()); dto.setTerminalId(a.getTerminalId());
        dto.setAddress(a.getAddress()); dto.setCity(a.getCity());
        dto.setInstitutionId(a.getInstitution().getId()); dto.setInstitutionName(a.getInstitution().getName());
        if (a.getCurrency1() != null) { dto.setCurrency1Id(a.getCurrency1().getId()); dto.setCurrency1Alpha(a.getCurrency1().getIsoAlpha()); }
        dto.setThreshold1(a.getThreshold1());
        if (a.getCurrency2() != null) { dto.setCurrency2Id(a.getCurrency2().getId()); dto.setCurrency2Alpha(a.getCurrency2().getIsoAlpha()); }
        dto.setThreshold2(a.getThreshold2());
        dto.setSvfePid(a.getSvfePid()); dto.setCreatedAt(a.getCreatedAt()); dto.setUpdatedAt(a.getUpdatedAt());
        snapshotRepository.findFirstByAtm_IdOrderBySnapshotTimeDesc(a.getId()).ifPresent(s -> {
            dto.setStatusId(s.getStatusId()); dto.setStatusDesc(s.getStatusDesc()); dto.setAllOk(s.getAllOk());
            dto.setThreshold1Exceeded(s.isThreshold1Exceeded()); dto.setThreshold2Exceeded(s.isThreshold2Exceeded());
            dto.setQuantity1(s.getQuantity1()); dto.setQuantity2(s.getQuantity2());
            dto.setQuantity3(s.getQuantity3()); dto.setQuantity4(s.getQuantity4());
        });
        statusJdbc.query("SELECT status_id,status_desc,status_key FROM atm_display_status WHERE atm_id=?", (org.springframework.jdbc.core.RowCallbackHandler) row -> {
            dto.setStatusId("unknown".equals(row.getString("status_key")) ? "UNKNOWN" : row.getString("status_id")); dto.setStatusDesc(row.getString("status_desc"));
        }, a.getId());
        return dto;
    }

    private AtmSnapshotDto toSnapshotDto(AtmSnapshot snapshot) {
        AtmSnapshotDto dto = new AtmSnapshotDto();
        dto.setId(snapshot.getId());
        dto.setAtmId(snapshot.getAtm().getId());
        dto.setTerminalId(snapshot.getAtm().getTerminalId());
        dto.setSnapshotTime(snapshot.getSnapshotTime());
        dto.setCurrency1Alpha(snapshot.getCurrency1() == null ? null : snapshot.getCurrency1().getIsoAlpha());
        dto.setNote1(snapshot.getNote1());
        dto.setQuantity1(snapshot.getQuantity1());
        dto.setCurrency2Alpha(snapshot.getCurrency2() == null ? null : snapshot.getCurrency2().getIsoAlpha());
        dto.setNote2(snapshot.getNote2());
        dto.setQuantity2(snapshot.getQuantity2());
        dto.setCurrency3Alpha(snapshot.getCurrency3() == null ? null : snapshot.getCurrency3().getIsoAlpha());
        dto.setNote3(snapshot.getNote3());
        dto.setQuantity3(snapshot.getQuantity3());
        dto.setCurrency4Alpha(snapshot.getCurrency4() == null ? null : snapshot.getCurrency4().getIsoAlpha());
        dto.setNote4(snapshot.getNote4());
        dto.setQuantity4(snapshot.getQuantity4());
        dto.setStatusId(snapshot.getStatusId());
        dto.setStatusDesc(snapshot.getStatusDesc());
        dto.setAllOk(snapshot.getAllOk());
        dto.setStatusMessage(snapshot.getStatusMessage());
        dto.setThreshold1Exceeded(snapshot.isThreshold1Exceeded());
        dto.setThreshold2Exceeded(snapshot.isThreshold2Exceeded());
        return dto;
    }
}
