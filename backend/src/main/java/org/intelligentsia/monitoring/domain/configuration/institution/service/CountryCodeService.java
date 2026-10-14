package org.intelligentsia.monitoring.domain.configuration.institution.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.configuration.institution.dto.CreateCountryCodeRequest;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.CountryCode;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.CountryCodeRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CountryCodeService {
    private final CountryCodeRepository repository;

    @Transactional(readOnly = true)
    public List<String> findAll() {
        return repository.findAll().stream().map(CountryCode::getCode).sorted().toList();
    }

    public String create(CreateCountryCodeRequest request) {
        String code = request.getCode().trim();
        if (repository.existsById(code)) {
            throw new BusinessException("COUNTRY_CODE_EXISTS", "Ce code numérique existe déjà.");
        }
        CountryCode countryCode = new CountryCode();
        countryCode.setCode(code);
        return repository.save(countryCode).getCode();
    }
}
