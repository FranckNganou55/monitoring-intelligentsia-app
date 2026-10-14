package org.intelligentsia.monitoring.domain.monitoring.atm.repository;

import org.intelligentsia.monitoring.domain.monitoring.atm.entity.Atm;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.AtmSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtmRepository extends JpaRepository<Atm, Long> {
    @Query(value = """
        SELECT a.* FROM atms a JOIN atm_display_status s ON s.atm_id=a.id
        WHERE (CAST(:institutionId AS bigint) IS NULL OR a.institution_id=:institutionId)
          AND (CAST(:search AS text) IS NULL OR LOWER(a.terminal_id) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(a.city) LIKE LOWER(CONCAT('%',:search,'%')))
          AND (CAST(:status AS text) IS NULL OR s.status_key=:status)
        """, nativeQuery = true, queryRewriter = AtmQueryRewriter.class)
    Page<Atm> findWithFilters(Long institutionId, String search, String status, Pageable pageable);
    @Query("SELECT a FROM Atm a WHERE a.institution.id = :institutionId ORDER BY a.terminalId ASC")
    List<Atm> findAllByInstitution(@Param("institutionId") Long institutionId);
    boolean existsByInstitutionIdAndTerminalId(Long institutionId, String terminalId);
}
