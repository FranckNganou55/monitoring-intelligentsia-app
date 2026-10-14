package org.intelligentsia.monitoring.domain.monitoring.fleet.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.fleet.dto.CardOverLimitDto;
import org.intelligentsia.monitoring.domain.monitoring.fleet.dto.FleetAccountDto;
import org.intelligentsia.monitoring.domain.monitoring.fleet.entity.CardOverLimit;
import org.intelligentsia.monitoring.domain.monitoring.fleet.entity.FleetAccount;
import org.intelligentsia.monitoring.domain.monitoring.fleet.repository.CardOverLimitRepository;
import org.intelligentsia.monitoring.domain.monitoring.fleet.repository.FleetAccountRepository;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FleetService {

    private final FleetAccountRepository fleetAccountRepository;
    private final CardOverLimitRepository cardOverLimitRepository;

    @Transactional(readOnly = true)
    public PageResponse<FleetAccountDto> findAccounts(FilterRequest filter, Long institutionId) {
        return PageResponse.of(
                fleetAccountRepository.findWithFilters(filter.getSearch(), institutionId, filter.toPageable())
                        .map(this::toAccountDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<CardOverLimitDto> findCardsOverLimit(FilterRequest filter, Long institutionId) {
        return PageResponse.of(
                cardOverLimitRepository.findWithFilters(filter.getSearch(), institutionId, filter.toPageable())
                        .map(this::toCardDto));
    }

    @Transactional
    public void save(FleetAccount account) {
        fleetAccountRepository.save(account);
    }

    @Transactional
    public void save(CardOverLimit card) {
        cardOverLimitRepository.save(card);
    }

    private FleetAccountDto toAccountDto(FleetAccount f) {
        FleetAccountDto dto = new FleetAccountDto();
        dto.setId(f.getId());
        if (f.getInstitution() != null) dto.setInstitutionId(f.getInstitution().getId());
        dto.setAccountNumber(f.getAccountNumber());
        if (f.getCurrency() != null) dto.setCurrency(f.getCurrency().getIsoAlpha());
        dto.setBalance(f.getBalance());
        dto.setPreviousBalance(f.getPreviousBalance());
        dto.setDelta1(f.getDelta1());
        dto.setDelta2(f.getDelta2());
        dto.setDelta1Exceeded(f.getDelta1Exceeded());
        dto.setDelta2Exceeded(f.getDelta2Exceeded());
        dto.setSnapshotTime(f.getSnapshotTime());
        return dto;
    }

    private CardOverLimitDto toCardDto(CardOverLimit c) {
        CardOverLimitDto dto = new CardOverLimitDto();
        dto.setId(c.getId());
        if (c.getInstitution() != null) dto.setInstitutionId(c.getInstitution().getId());
        dto.setCardNumber(c.getCardNumber());
        if (c.getCurrency() != null) dto.setCurrency(c.getCurrency().getIsoAlpha());
        dto.setLimitAmount(c.getLimitAmount());
        dto.setCurrentAmount(c.getCurrentAmount());
        dto.setExceeded(c.getExceeded());
        dto.setSnapshotTime(c.getSnapshotTime());
        return dto;
    }
}
