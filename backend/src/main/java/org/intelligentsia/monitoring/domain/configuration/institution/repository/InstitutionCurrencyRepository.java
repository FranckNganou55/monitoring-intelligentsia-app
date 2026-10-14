package org.intelligentsia.monitoring.domain.configuration.institution.repository;

import org.intelligentsia.monitoring.domain.configuration.institution.entity.InstitutionCurrency;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.InstitutionCurrencyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InstitutionCurrencyRepository extends JpaRepository<InstitutionCurrency, InstitutionCurrencyId> {

    @Query("SELECT ic FROM InstitutionCurrency ic WHERE ic.institution.id = :institutionId")
    List<InstitutionCurrency> findByInstitutionId(@Param("institutionId") Long institutionId);
}
