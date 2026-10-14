package org.intelligentsia.monitoring.domain.monitoring.alert.repository;

import org.intelligentsia.monitoring.domain.monitoring.alert.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertEventRepository
        extends JpaRepository<AlertEvent, Long> {

    List<AlertEvent> findByAlertIdOrderByCreatedAtAsc(
            Long alertId
    );
}