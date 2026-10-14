package org.intelligentsia.monitoring.domain.configuration.scheduler.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.configuration.scheduler.dto.SchedulerTaskDto;
import org.intelligentsia.monitoring.domain.configuration.scheduler.entity.SchedulerTask;
import org.intelligentsia.monitoring.domain.configuration.scheduler.repository.SchedulerTaskRepository;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class SchedulerManagementService {
    private final SchedulerTaskRepository repo;

    @Transactional(readOnly = true)
    public List<SchedulerTaskDto> findAll() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public SchedulerTaskDto toggleEnabled(Long id, boolean enabled) {
        SchedulerTask t = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tâche", id));
        t.setEnabled(enabled); return toDto(repo.save(t));
    }

    @Transactional
    public SchedulerTaskDto updateInterval(Long id, long intervalMs) {
        SchedulerTask t = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tâche", id));
        t.setIntervalMs(intervalMs); return toDto(repo.save(t));
    }

    private SchedulerTaskDto toDto(SchedulerTask t) {
        SchedulerTaskDto dto = new SchedulerTaskDto();
        dto.setId(t.getId()); dto.setCode(t.getCode()); dto.setName(t.getName());
        dto.setEnabled(t.isEnabled()); dto.setIntervalMs(t.getIntervalMs());
        dto.setLastRun(t.getLastRun()); dto.setLastStatus(t.getLastStatus()); dto.setLastError(t.getLastError());
        return dto;
    }
}
