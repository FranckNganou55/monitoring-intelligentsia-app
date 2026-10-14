package org.intelligentsia.monitoring.domain.monitoring.pos.repository;

import org.intelligentsia.monitoring.domain.monitoring.pos.entity.Technician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    List<Technician> findByInstitutionId(Long institutionId);

    @Query("""
        SELECT t FROM Technician t WHERE
        (:institutionId IS NULL OR t.institution.id = :institutionId)
        AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%',:search,'%'))
             OR LOWER(t.phone) LIKE LOWER(CONCAT('%',:search,'%')))
        """)
    Page<Technician> findWithFilters(Long institutionId, String search, Pageable pageable);
}
