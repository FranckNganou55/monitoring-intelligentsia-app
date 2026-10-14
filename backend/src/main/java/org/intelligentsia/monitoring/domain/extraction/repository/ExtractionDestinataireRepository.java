package org.intelligentsia.monitoring.domain.extraction.repository;

import org.intelligentsia.monitoring.domain.extraction.entity.ExtractionDestinataire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractionDestinataireRepository extends JpaRepository<ExtractionDestinataire, Long> {
    List<ExtractionDestinataire> findByConfigIdAndActiveTrue(Long configId);
}
