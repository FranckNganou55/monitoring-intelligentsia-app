package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsHost;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsHostRepository extends JpaRepository<CmsHost, String> {

    @Query(value = "SELECT I.INT_IDEN pid, I.INT_LABE description,"
            + " DECODE(I.INT_STAT, 0, 'D', 1, 'U', 3, 'D', 4, 'U', 'S') opStat"
            + " FROM INTERFACES I WHERE I.INT_ITY_CODE IN (8, 13)"
            + " OR UPPER(I.INT_IDEN) LIKE 'HOST%' OR UPPER(I.INT_IDEN) LIKE 'HSM%'", nativeQuery = true)
    List<CmsHost> findHost();
}
