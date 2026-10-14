package org.intelligentsia.monitoring.domain.configuration.scheduler.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.configuration.scheduler.dto.SchedulerTaskDto;
import org.intelligentsia.monitoring.domain.configuration.scheduler.service.SchedulerManagementService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Planificateur") @RestController @RequestMapping("/api/scheduler") @RequiredArgsConstructor
public class SchedulerController {
    private final SchedulerManagementService service;

    @GetMapping @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<SchedulerTaskDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll()));
    }

    @PatchMapping("/{id}/toggle") @PreAuthorize("hasAuthority('SCHEDULER_WRITE')")
    public ResponseEntity<ApiResponse<SchedulerTaskDto>> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(service.toggleEnabled(id, enabled)));
    }

    @PatchMapping("/{id}/interval") @PreAuthorize("hasAuthority('SCHEDULER_WRITE')")
    public ResponseEntity<ApiResponse<SchedulerTaskDto>> updateInterval(@PathVariable Long id, @RequestParam long intervalMs) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateInterval(id, intervalMs)));
    }
}
