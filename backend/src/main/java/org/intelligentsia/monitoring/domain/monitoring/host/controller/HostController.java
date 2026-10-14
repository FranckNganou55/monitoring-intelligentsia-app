package org.intelligentsia.monitoring.domain.monitoring.host.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.host.dto.HostSnapshotDto;
import org.intelligentsia.monitoring.domain.monitoring.host.service.HostService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Hôtes") @RestController @RequestMapping("/api/host") @RequiredArgsConstructor
public class HostController {
    private final HostService service;

    @GetMapping("/latest") @PreAuthorize("hasAuthority('HOST_READ')")
    public ResponseEntity<ApiResponse<List<HostSnapshotDto>>> latest(
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findLatest(instId)));
    }

    @GetMapping("/history") @PreAuthorize("hasAuthority('HOST_READ')")
    public ResponseEntity<ApiResponse<PageResponse<HostSnapshotDto>>> history(
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
