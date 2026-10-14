package org.intelligentsia.monitoring.domain.monitoring.extraction.repository;

import org.intelligentsia.monitoring.domain.monitoring.extraction.entity.ExtractionBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExtractionRepository extends JpaRepository<ExtractionBatch, Long> {
    @Query("""
        SELECT b FROM ExtractionBatch b WHERE
        (:institutionId IS NULL OR b.institution.id = :institutionId)
        AND (:type IS NULL OR b.type = :type)
        """)
    Page<ExtractionBatch> findWithFilters(Long institutionId, String type, Pageable pageable);
}
