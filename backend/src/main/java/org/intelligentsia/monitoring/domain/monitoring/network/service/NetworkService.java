package org.intelligentsia.monitoring.domain.monitoring.network.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.network.dto.NetworkSnapshotDto;
import org.intelligentsia.monitoring.domain.monitoring.network.entity.NetworkSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.network.repository.NetworkSnapshotRepository;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class NetworkService {
    private final NetworkSnapshotRepository repository;

    @Transactional(readOnly = true)
    public List<NetworkSnapshotDto> findLatest(Long institutionId) {
        if (institutionId != null) return List.of();
        return repository.findAllLatest().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<NetworkSnapshotDto> findHistory(Long institutionId, Pageable pageable) {
        if (institutionId != null) return PageResponse.of(org.springframework.data.domain.Page.empty(pageable));
        return PageResponse.of(repository.findHistory(pageable).map(this::toDto));
    }

    private NetworkSnapshotDto toDto(NetworkSnapshot s) {
        NetworkSnapshotDto dto = new NetworkSnapshotDto();
        dto.setId(s.getId()); dto.setNetworkCode(s.getNetworkCode()); dto.setDescription(s.getDescription());
        dto.setOperatingStatus(s.getOperatingStatus()); dto.setProcessingStatus(s.getProcessingStatus());
        dto.setTransactionMode(s.getTransactionMode()); dto.setSnapshotTime(s.getSnapshotTime());
        return dto;
    }
}
