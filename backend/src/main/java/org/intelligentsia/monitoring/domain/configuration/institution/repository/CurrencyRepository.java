package org.intelligentsia.monitoring.domain.configuration.institution.repository;

import org.intelligentsia.monitoring.domain.configuration.institution.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    Optional<Currency> findByIsoAlpha(String isoAlpha);
    Optional<Currency> findByIsoNumeric(String isoNumeric);
}
