package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsNetwork;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsNetworkRepository extends JpaRepository<CmsNetwork, String> {

    /** Typo intentionally preserved to match the Oracle view/stored procedure name */
    @Query(value = "SELECT int_code AS pid, int_labe AS description,"
            + " DECODE(int_stat, 0, 'D', 1, 'U', 3, 'D', 4, 'U', 'S') AS opStat"
            + " FROM interfaces i INNER JOIN interface_status si ON i.int_stat = si.ist_code"
            + " WHERE int_code IN (30, 40, 45, 50, 61, 63)", nativeQuery = true)
    List<CmsNetwork> findNetwok();
}
