package org.intelligentsia.monitoring.domain.monitoring.atm.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.*;
import org.intelligentsia.monitoring.domain.monitoring.atm.service.AtmService;
import org.intelligentsia.monitoring.shared.annotation.Auditable;
import org.intelligentsia.monitoring.shared.dto.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ATMs") @RestController @RequestMapping("/api/atms") @RequiredArgsConstructor
public class AtmController {
    private final AtmService service;
    private final org.intelligentsia.monitoring.domain.monitoring.atm.service.AtmMovementService movements;
    private final org.intelligentsia.monitoring.domain.monitoring.atm.service.AtmReportService reports;

    @GetMapping("/{id}/movements") @PreAuthorize("hasAuthority('ATM_READ')")
    public ResponseEntity<ApiResponse<PageResponse<AtmMovementDto>>> movements(
            @PathVariable Long id, @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) java.time.Instant from,
            @RequestParam(required = false) java.time.Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(movements.page(id, effectiveId, status, from, to, page, size)));
    }

    @GetMapping({"/report", "/{id}/movements/export"}) @PreAuthorize("hasAuthority('ATM_READ')")
    public ResponseEntity<byte[]> report(
            @PathVariable(required = false) Long id,
            @RequestParam(required = false) Long institutionId, @RequestParam(required = false) String search,
            @RequestParam(required = false) String status, @RequestParam(required = false) String movementStatus,
            @RequestParam(required = false) java.time.Instant from, @RequestParam(required = false) java.time.Instant to,
            @RequestParam(defaultValue = "PDF") String format, @AuthenticationPrincipal User user) throws java.io.IOException {
        String normalized = format.trim().toUpperCase(java.util.Locale.ROOT);
        String extension = switch (normalized) { case "PDF" -> "pdf"; case "WORD" -> "docx"; case "EXCEL" -> "xlsx";
            default -> throw new org.intelligentsia.monitoring.infrastructure.exception.BusinessException("INVALID_FORMAT", "Format invalide."); };
        String mime = switch (normalized) { case "PDF" -> "application/pdf"; case "WORD" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; };
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport-gabs." + extension + "\"")
                .body(reports.export(id, effectiveId, search, status, movementStatus, from, to, normalized));
    }

    @GetMapping @PreAuthorize("hasAuthority('ATM_READ')")
    public ResponseEntity<ApiResponse<PageResponse<AtmDto>>> list(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(filter, effectiveId, status)));
    }

    @GetMapping("/by-institution/{institutionId}") @PreAuthorize("hasAuthority('ATM_READ')")
    public ResponseEntity<ApiResponse<java.util.List<AtmDto>>> listByInstitution(
            @PathVariable Long institutionId,
            @RequestParam(name = "institutionId", required = false) Long selectedInstitutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : (selectedInstitutionId != null ? selectedInstitutionId : institutionId);
        return ResponseEntity.ok(ApiResponse.ok(service.findAllByInstitution(effectiveId)));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('ATM_READ')")
    public ResponseEntity<ApiResponse<AtmDto>> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id, effectiveId)));
    }

    @GetMapping("/{id}/snapshots/latest") @PreAuthorize("hasAuthority('ATM_READ')")
    public ResponseEntity<ApiResponse<AtmSnapshotDto>> latestSnapshot(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findLatestSnapshot(id, effectiveId)));
    }

    @GetMapping("/{id}/snapshots") @PreAuthorize("hasAuthority('ATM_READ')")
    public ResponseEntity<ApiResponse<java.util.List<AtmSnapshotDto>>> snapshots(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findSnapshots(id, effectiveId, limit)));
    }

    @PostMapping @PreAuthorize("hasAuthority('ATM_WRITE')") @Auditable(action = "CREATE", entityType = "ATM")
    public ResponseEntity<ApiResponse<AtmDto>> create(@Valid @RequestBody AtmCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(req)));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('ATM_WRITE')") @Auditable(action = "UPDATE", entityType = "ATM")
    public ResponseEntity<ApiResponse<AtmDto>> update(@PathVariable Long id, @Valid @RequestBody AtmCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('ATM_WRITE')") @Auditable(action = "DELETE", entityType = "ATM")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.ok(ApiResponse.<Void>ok("ATM supprimé."));
    }
}
