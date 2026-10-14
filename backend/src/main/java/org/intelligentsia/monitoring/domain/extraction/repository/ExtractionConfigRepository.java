package org.intelligentsia.monitoring.domain.extraction.repository;

import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExtractionConfigRepository extends JpaRepository<ExtractionConfig, Long> {

    @Query("""
            SELECT c FROM ExtractionConfig c
            WHERE (:institutionId IS NULL OR c.institution.id = :institutionId)
              AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%',:search,'%')))
            """)
    Page<ExtractionConfig> findWithFilters(Long institutionId, String search, Pageable pageable);

    List<ExtractionConfig> findByActiveTrue();

    List<ExtractionConfig> findByActiveTrueAndCronExpressionIsNotNull();
}
