package org.intelligentsia.monitoring.domain.extraction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.infrastructure.tenant.TenantContext;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionConfigDto;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionConfigRequest;
import org.intelligentsia.monitoring.domain.extraction.service.ExtractionConfigService;
import org.intelligentsia.monitoring.domain.extraction.service.ExtractionSchedulerService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/extraction/configs")
@RequiredArgsConstructor
public class ExtractionConfigController {

    private final ExtractionConfigService configService;
    private final ExtractionSchedulerService schedulerService;

    @GetMapping
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<ExtractionConfigDto>>> list(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : (TenantContext.get() != null ? TenantContext.get() : institutionId);
        return ResponseEntity.ok(ApiResponse.ok(configService.findAll(filter, effectiveId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<ExtractionConfigDto>> get(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : (TenantContext.get() != null ? TenantContext.get() : institutionId);
        return ResponseEntity.ok(ApiResponse.ok(configService.findById(id, effectiveId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXTRACTION_WRITE')")
    public ResponseEntity<ApiResponse<ExtractionConfigDto>> create(
            @Valid @RequestBody ExtractionConfigRequest req) {
        ExtractionConfigDto dto = configService.create(req);
        if (dto.getCronExpression() != null && dto.isActive()) {
            schedulerService.schedule(dto.getId(), dto.getCronExpression());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXTRACTION_WRITE')")
    public ResponseEntity<ApiResponse<ExtractionConfigDto>> update(
            @PathVariable Long id, @Valid @RequestBody ExtractionConfigRequest req) {
        ExtractionConfigDto dto = configService.update(id, req);
        if (dto.getCronExpression() != null && dto.isActive()) {
            schedulerService.schedule(dto.getId(), dto.getCronExpression());
        } else {
            schedulerService.cancel(dto.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXTRACTION_WRITE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        schedulerService.cancel(id);
        configService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Configuration supprimée."));
    }
}
