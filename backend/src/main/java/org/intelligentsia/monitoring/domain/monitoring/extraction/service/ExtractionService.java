package org.intelligentsia.monitoring.domain.monitoring.extraction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.monitoring.extraction.dto.ExtractionBatchDto;
import org.intelligentsia.monitoring.domain.monitoring.extraction.dto.ExtractionCreateRequest;
import org.intelligentsia.monitoring.domain.monitoring.extraction.entity.ExtractionBatch;
import org.intelligentsia.monitoring.domain.monitoring.extraction.entity.ExtractionTerminal;
import org.intelligentsia.monitoring.domain.monitoring.extraction.repository.ExtractionRepository;
import org.intelligentsia.monitoring.domain.monitoring.extraction.repository.ExtractionTransactionRepository;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class ExtractionService {
    private final ExtractionRepository extractionRepository;
    private final ExtractionTransactionRepository transactionRepository;
    private final InstitutionRepository institutionRepository;

    @Transactional(readOnly = true)
    public PageResponse<ExtractionBatchDto> findAll(FilterRequest filter, Long institutionId, String type) {
        return PageResponse.of(extractionRepository.findWithFilters(institutionId, type, filter.toPageable()).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ExtractionBatchDto findById(Long id) {
        return toDto(extractionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ExtractionBatch", id)));
    }

    @Transactional
    public ExtractionBatchDto create(ExtractionCreateRequest req, Long currentUserId) {
        Institution inst = institutionRepository.findById(req.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", req.getInstitutionId()));
        ExtractionBatch batch = new ExtractionBatch();
        batch.setInstitution(inst); batch.setType(req.getType());
        batch.setStartDate(req.getStartDate()); batch.setEndDate(req.getEndDate());
        batch.setMerchant(req.getMerchant()); batch.setCity(req.getCity());
        batch.setCreatedAt(Instant.now()); batch.setCreatedBy(currentUserId);
        if (req.getTerminalIds() != null) {
            List<ExtractionTerminal> terminals = new ArrayList<>();
            for (String tid : req.getTerminalIds()) {
                ExtractionTerminal et = new ExtractionTerminal();
                et.setBatch(batch); et.setTerminalId(tid); terminals.add(et);
            }
            batch.setTerminals(terminals);
        }
        return toDto(extractionRepository.save(batch));
    }

    @Transactional
    public void delete(Long id) {
        if (!extractionRepository.existsById(id)) throw new ResourceNotFoundException("ExtractionBatch", id);
        extractionRepository.deleteById(id);
    }

    private ExtractionBatchDto toDto(ExtractionBatch b) {
        ExtractionBatchDto dto = new ExtractionBatchDto();
        dto.setId(b.getId()); dto.setType(b.getType());
        dto.setStartDate(b.getStartDate()); dto.setEndDate(b.getEndDate());
        dto.setMerchant(b.getMerchant()); dto.setCity(b.getCity());
        dto.setCreatedAt(b.getCreatedAt()); dto.setCreatedBy(b.getCreatedBy());
        dto.setInstitutionId(b.getInstitution().getId()); dto.setInstitutionName(b.getInstitution().getName());
        dto.setTerminalIds(b.getTerminals().stream().map(ExtractionTerminal::getTerminalId).toList());
        dto.setTransactionCount(transactionRepository.countByBatchId(b.getId()));
        return dto;
    }
}
