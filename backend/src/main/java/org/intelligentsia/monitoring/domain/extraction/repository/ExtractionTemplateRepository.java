package org.intelligentsia.monitoring.domain.extraction.repository;

import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExtractionTemplateRepository extends JpaRepository<ExtractionTemplate, Long> {

    @Query("SELECT t FROM ExtractionTemplate t WHERE (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(t.module) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<ExtractionTemplate> findWithFilters(String search, Pageable pageable);

    List<ExtractionTemplate> findByActiveTrue();

    List<ExtractionTemplate> findByActiveTrueOrderByNameAsc();
}
