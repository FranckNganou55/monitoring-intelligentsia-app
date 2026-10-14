package org.intelligentsia.monitoring.domain.monitoring.atm.repository;

import org.intelligentsia.monitoring.domain.monitoring.atm.entity.AtmInterfaceMonitoringState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtmInterfaceMonitoringStateRepository
        extends JpaRepository<AtmInterfaceMonitoringState, String> {
}
