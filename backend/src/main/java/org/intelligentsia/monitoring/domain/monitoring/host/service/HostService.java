package org.intelligentsia.monitoring.domain.monitoring.host.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.host.dto.HostSnapshotDto;
import org.intelligentsia.monitoring.domain.monitoring.host.entity.HostSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.host.repository.HostSnapshotRepository;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class HostService {
    private final HostSnapshotRepository repository;

    @Transactional(readOnly = true)
    public List<HostSnapshotDto> findLatest(Long institutionId) {
        if (institutionId != null) return List.of();
        return repository.findAllLatest().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<HostSnapshotDto> findHistory(Long institutionId, Pageable pageable) {
        if (institutionId != null) return PageResponse.of(org.springframework.data.domain.Page.empty(pageable));
        return PageResponse.of(repository.findHistory(pageable).map(this::toDto));
    }

    private HostSnapshotDto toDto(HostSnapshot s) {
        HostSnapshotDto dto = new HostSnapshotDto();
        dto.setId(s.getId()); dto.setHostname(s.getHostname()); dto.setDescription(s.getDescription()); dto.setIpAddress(s.getIpAddress());
        dto.setOperatingStatus(s.getOperatingStatus()); dto.setProcessingStatus(s.getProcessingStatus());
        dto.setSnapshotTime(s.getSnapshotTime());
        return dto;
    }
}
