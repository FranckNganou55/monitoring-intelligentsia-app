package org.intelligentsia.monitoring.domain.extraction.repository;

import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionJob;
import org.intelligentsia.monitoring.domain.extraction.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, Long> {

    @Query("""
            SELECT j FROM ExtractionJob j
            WHERE (:configId IS NULL OR j.config.id = :configId)
              AND (:institutionId IS NULL OR j.config.institution.id = :institutionId)
              AND (:status IS NULL OR j.status = :status)
            ORDER BY j.createdAt DESC
            """)
    Page<ExtractionJob> findWithFilters(Long configId, Long institutionId, JobStatus status, Pageable pageable);

    /** Eager-loads all lazy associations needed by executeJob() to avoid LazyInitializationException in async context. */
    @Query("""
            SELECT j FROM ExtractionJob j
            JOIN FETCH j.config c
            JOIN FETCH c.template
            JOIN FETCH c.institution
            LEFT JOIN FETCH c.destinataires
            WHERE j.id = :id
            """)
    Optional<ExtractionJob> findByIdEager(@Param("id") Long id);
}
