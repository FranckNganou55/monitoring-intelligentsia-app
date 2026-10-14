package org.intelligentsia.monitoring.domain.configuration.institution.repository;

import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    @Query("SELECT i FROM Institution i WHERE :search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%',:search,'%'))")
    Page<Institution> findWithSearch(String search, Pageable pageable);
}
