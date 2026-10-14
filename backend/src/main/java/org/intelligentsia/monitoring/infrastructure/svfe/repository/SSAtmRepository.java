package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.SSAtm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Alias for CmsAtmRepository — both map to the SSAtm Oracle table.
 * Prefer CmsAtmRepository.findATMs() for the standard ATM sync query.
 */
@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface SSAtmRepository extends JpaRepository<SSAtm, Integer> {
}
