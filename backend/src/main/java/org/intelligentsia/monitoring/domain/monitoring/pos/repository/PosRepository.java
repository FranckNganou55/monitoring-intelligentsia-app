package org.intelligentsia.monitoring.domain.monitoring.pos.repository;

import org.intelligentsia.monitoring.domain.monitoring.pos.entity.PosTerminal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PosRepository extends JpaRepository<PosTerminal, Long> {
    boolean existsByTerminalId(String terminalId);

    @Query("""
        SELECT p FROM PosTerminal p WHERE
        (:institutionId IS NULL OR p.institution.id = :institutionId)
        AND (:search IS NULL OR LOWER(p.terminalId) LIKE LOWER(CONCAT('%',:search,'%'))
             OR LOWER(p.merchant) LIKE LOWER(CONCAT('%',:search,'%'))
             OR LOWER(p.city) LIKE LOWER(CONCAT('%',:search,'%')))
        """)
    Page<PosTerminal> findWithFilters(Long institutionId, String search, Pageable pageable);
}
