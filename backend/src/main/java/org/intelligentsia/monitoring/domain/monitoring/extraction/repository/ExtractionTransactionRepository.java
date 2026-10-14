package org.intelligentsia.monitoring.domain.monitoring.extraction.repository;

import org.intelligentsia.monitoring.domain.monitoring.extraction.entity.ExtractionTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExtractionTransactionRepository extends JpaRepository<ExtractionTransaction, Long> {
    List<ExtractionTransaction> findByBatchId(Long batchId);
    Page<ExtractionTransaction> findByBatchId(Long batchId, Pageable pageable);
    long countByBatchId(Long batchId);
    void deleteByBatchId(Long batchId);
}
