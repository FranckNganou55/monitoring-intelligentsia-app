package org.intelligentsia.monitoring.domain.monitoring.extraction.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.extraction.dto.ExtractionBatchDto;
import org.intelligentsia.monitoring.domain.monitoring.extraction.dto.ExtractionCreateRequest;
import org.intelligentsia.monitoring.domain.monitoring.extraction.service.ExtractionService;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.annotation.Auditable;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Extractions") @RestController @RequestMapping("/api/extractions") @RequiredArgsConstructor
public class ExtractionController {
    private final ExtractionService service;

    @GetMapping @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<ExtractionBatchDto>>> list(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal User user) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(filter, instId, type)));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<ExtractionBatchDto>> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        ExtractionBatchDto batch = service.findById(id);
        if (effectiveId != null && !effectiveId.equals(batch.getInstitutionId())) {
            throw new ResourceNotFoundException("ExtractionBatch", id);
        }
        return ResponseEntity.ok(ApiResponse.ok(batch));
    }

    @PostMapping @PreAuthorize("hasAuthority('EXTRACTION_WRITE')") @Auditable(action = "CREATE", entityType = "EXTRACTION")
    public ResponseEntity<ApiResponse<ExtractionBatchDto>> create(
            @Valid @RequestBody ExtractionCreateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(req, user.getId())));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('EXTRACTION_WRITE')") @Auditable(action = "DELETE", entityType = "EXTRACTION")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.ok(ApiResponse.<Void>ok("Extraction supprimée."));
    }
}
