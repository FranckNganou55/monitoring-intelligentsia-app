package org.intelligentsia.monitoring.domain.extraction.controller;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionJobDto;
import org.intelligentsia.monitoring.domain.extraction.service.ExtractionEngineService;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.infrastructure.tenant.TenantContext;
import org.intelligentsia.monitoring.shared.annotation.TenantAware;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/extraction/jobs")
@RequiredArgsConstructor
public class ExtractionJobController {

    private final ExtractionEngineService engineService;

    @GetMapping
    @TenantAware
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<ExtractionJobDto>>> list(
            FilterRequest filter,
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : (TenantContext.get() != null ? TenantContext.get() : institutionId);
        return ResponseEntity.ok(ApiResponse.ok(engineService.findJobs(filter, configId, effectiveId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<ExtractionJobDto>> get(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : (TenantContext.get() != null ? TenantContext.get() : institutionId);
        ExtractionJobDto job = engineService.findJobById(id);
        if (effectiveId != null && !effectiveId.equals(job.getInstitutionId())) {
            throw new ResourceNotFoundException("ExtractionJob", id);
        }
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @PostMapping("/trigger/{configId}")
    @PreAuthorize("hasAuthority('EXTRACTION_WRITE')")
    public ResponseEntity<ApiResponse<ExtractionJobDto>> trigger(
            @PathVariable Long configId,
            @RequestBody(required = false) Map<String, String> extraParams) {
        ExtractionJobDto job = engineService.triggerManual(configId,
                extraParams != null ? extraParams : Map.of());
        return ResponseEntity.accepted().body(ApiResponse.ok("Extraction démarrée.", job));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Path file = engineService.getJobFile(id);
        Resource resource = new PathResource(file);
        String filename = file.getFileName().toString();
        String mediaType = filename.endsWith(".xlsx")
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv; charset=UTF-8";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(mediaType))
                .body(resource);
    }
}
