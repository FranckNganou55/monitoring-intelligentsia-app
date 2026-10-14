package org.intelligentsia.monitoring.domain.monitoring.fraud.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.fraud.dto.FraudDetailDto;
import org.intelligentsia.monitoring.domain.monitoring.fraud.dto.FraudRateDto;
import org.intelligentsia.monitoring.domain.monitoring.fraud.entity.FraudDetail;
import org.intelligentsia.monitoring.domain.monitoring.fraud.entity.FraudRate;
import org.intelligentsia.monitoring.domain.monitoring.fraud.repository.FraudDetailRepository;
import org.intelligentsia.monitoring.domain.monitoring.fraud.repository.FraudRateRepository;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FraudService {

    private final FraudRateRepository fraudRateRepository;
    private final FraudDetailRepository fraudDetailRepository;

    @Transactional(readOnly = true)
    public PageResponse<FraudRateDto> findRates(FilterRequest filter, Long institutionId) {
        var rates = fraudRateRepository.findWithFilters(
                filter.getSearch(), institutionId, filter.toPageable());
        if (rates.getTotalElements() > 0) {
            return PageResponse.of(rates.map(this::toRateDto));
        }

        // The dashboard and detail view are backed by fraud_details. Use those
        // snapshots when a separate fraud_rates feed has not been populated.
        return PageResponse.of(
                fraudDetailRepository.findWithFilters(
                        filter.getSearch(), institutionId, filter.toPageable())
                        .map(this::toRateDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<FraudDetailDto> findDetails(FilterRequest filter, Long institutionId) {
        return PageResponse.of(
                fraudDetailRepository.findWithFilters(filter.getSearch(), institutionId, filter.toPageable())
                        .map(this::toDetailDto));
    }

    @Transactional
    public void save(FraudRate rate) {
        fraudRateRepository.save(rate);
    }

    @Transactional
    public void saveDetail(FraudDetail detail) {
        fraudDetailRepository.save(detail);
    }

    private FraudRateDto toRateDto(FraudRate r) {
        FraudRateDto dto = new FraudRateDto();
        dto.setId(r.getId());
        if (r.getInstitution() != null) dto.setInstitutionId(r.getInstitution().getId());
        dto.setBin(r.getBin());
        dto.setSuspectCount(r.getSuspectCount());
        dto.setFraudCount(r.getFraudCount());
        dto.setSnapshotTime(r.getSnapshotTime());
        return dto;
    }

    private FraudRateDto toRateDto(FraudDetail d) {
        FraudRateDto dto = new FraudRateDto();
        dto.setId(d.getId());
        if (d.getInstitution() != null) dto.setInstitutionId(d.getInstitution().getId());
        dto.setBin(d.getBin());
        dto.setSuspectCount(d.getSuspectCount());
        dto.setFraudCount(d.getFraudCount());
        dto.setSnapshotTime(d.getSnapshotTime());
        return dto;
    }

    private FraudDetailDto toDetailDto(FraudDetail d) {
        FraudDetailDto dto = new FraudDetailDto();
        dto.setId(d.getId());
        if (d.getInstitution() != null) dto.setInstitutionId(d.getInstitution().getId());
        dto.setBin(d.getBin());
        dto.setRuleCode(d.getRuleCode());
        dto.setRuleDesc(d.getRuleDesc());
        dto.setSuspectCount(d.getSuspectCount());
        dto.setFraudCount(d.getFraudCount());
        dto.setBins(d.getBins());
        dto.setDescription(d.getDescription());
        dto.setSnapshotTime(d.getSnapshotTime());
        return dto;
    }
}
