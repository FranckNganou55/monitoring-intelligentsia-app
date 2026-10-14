package org.intelligentsia.monitoring.domain.configuration.scheduler.repository;

import org.intelligentsia.monitoring.domain.configuration.scheduler.entity.SchedulerTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SchedulerTaskRepository extends JpaRepository<SchedulerTask, Long> {
    Optional<SchedulerTask> findByCode(String code);
    List<SchedulerTask> findByEnabledTrue();
}
