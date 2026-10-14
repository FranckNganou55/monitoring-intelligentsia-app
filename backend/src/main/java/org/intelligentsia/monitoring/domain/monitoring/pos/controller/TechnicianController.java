package org.intelligentsia.monitoring.domain.monitoring.pos.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.TechnicianCreateRequest;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.TechnicianDto;
import org.intelligentsia.monitoring.domain.monitoring.pos.service.TechnicianService;
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
import java.util.List;

@Tag(name = "Techniciens") @RestController @RequestMapping("/api/technicians") @RequiredArgsConstructor
public class TechnicianController {
    private final TechnicianService service;

    @GetMapping @PreAuthorize("hasAuthority('POS_READ')")
    public ResponseEntity<ApiResponse<PageResponse<TechnicianDto>>> list(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(filter, instId)));
    }

    @GetMapping("/by-institution/{institutionId}") @PreAuthorize("hasAuthority('POS_READ')")
    public ResponseEntity<ApiResponse<List<TechnicianDto>>> listByInstitution(
            @PathVariable Long institutionId,
            @RequestParam(name = "institutionId", required = false) Long selectedInstitutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution() ? user.getInstitution().getId()
                : (selectedInstitutionId != null ? selectedInstitutionId : institutionId);
        return ResponseEntity.ok(ApiResponse.ok(service.findByInstitution(effectiveId)));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('POS_READ')")
    public ResponseEntity<ApiResponse<TechnicianDto>> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        TechnicianDto technician = service.findById(id);
        if (effectiveId != null && !effectiveId.equals(technician.getInstitutionId())) {
            throw new ResourceNotFoundException("Technicien", id);
        }
        return ResponseEntity.ok(ApiResponse.ok(technician));
    }

    @PostMapping @PreAuthorize("hasAuthority('POS_WRITE')") @Auditable(action = "CREATE", entityType = "TECHNICIAN")
    public ResponseEntity<ApiResponse<TechnicianDto>> create(@Valid @RequestBody TechnicianCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(req)));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('POS_WRITE')") @Auditable(action = "UPDATE", entityType = "TECHNICIAN")
    public ResponseEntity<ApiResponse<TechnicianDto>> update(@PathVariable Long id, @Valid @RequestBody TechnicianCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('POS_WRITE')") @Auditable(action = "DELETE", entityType = "TECHNICIAN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.ok(ApiResponse.<Void>ok("Technicien supprimé."));
    }
}
