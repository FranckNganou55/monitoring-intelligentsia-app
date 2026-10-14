package org.intelligentsia.monitoring.domain.monitoring.network.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.network.dto.NetworkSnapshotDto;
import org.intelligentsia.monitoring.domain.monitoring.network.service.NetworkService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Réseau") @RestController @RequestMapping("/api/network") @RequiredArgsConstructor
public class NetworkController {
    private final NetworkService service;

    @GetMapping("/latest") @PreAuthorize("hasAuthority('NETWORK_READ')")
    public ResponseEntity<ApiResponse<List<NetworkSnapshotDto>>> latest(
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findLatest(instId)));
    }

    @GetMapping("/history") @PreAuthorize("hasAuthority('NETWORK_READ')")
    public ResponseEntity<ApiResponse<PageResponse<NetworkSnapshotDto>>> history(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findHistory(instId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "snapshotTime")))));
    }
}
