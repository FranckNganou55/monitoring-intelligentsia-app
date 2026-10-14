package org.intelligentsia.monitoring.domain.monitoring.extraction.repository;

import org.intelligentsia.monitoring.domain.monitoring.extraction.entity.ExtractionTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExtractionTerminalRepository extends JpaRepository<ExtractionTerminal, Long> {
    List<ExtractionTerminal> findByBatchId(Long batchId);
    void deleteByBatchId(Long batchId);
}
