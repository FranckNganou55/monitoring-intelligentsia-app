package org.intelligentsia.monitoring.domain.configuration.institution.repository;

import org.intelligentsia.monitoring.domain.configuration.institution.entity.CountryCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryCodeRepository extends JpaRepository<CountryCode, String> {
}
