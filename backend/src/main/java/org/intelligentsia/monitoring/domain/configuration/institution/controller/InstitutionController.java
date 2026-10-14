package org.intelligentsia.monitoring.domain.configuration.institution.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.configuration.institution.dto.*;
import org.intelligentsia.monitoring.domain.configuration.institution.service.InstitutionService;
import org.intelligentsia.monitoring.shared.annotation.Auditable;
import org.intelligentsia.monitoring.shared.dto.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Institutions") @RestController @RequestMapping("/api/institutions") @RequiredArgsConstructor
public class InstitutionController {
    private final InstitutionService service;

    @GetMapping @PreAuthorize("hasAuthority('INSTITUTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<InstitutionDto>>> list(FilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(filter)));
    }

    @GetMapping("/all") @PreAuthorize("hasAuthority('INSTITUTION_READ')")
    public ResponseEntity<ApiResponse<List<InstitutionDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAllForSelect()));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('INSTITUTION_READ')")
    public ResponseEntity<ApiResponse<InstitutionDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @PatchMapping("/{id}/alert-routing") @PreAuthorize("hasAuthority('INSTITUTION_WRITE')")
    @Auditable(action = "UPDATE_ALERT_ROUTING", entityType = "INSTITUTION")
    public ResponseEntity<ApiResponse<InstitutionDto>> updateAlertRouting(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionAlertRoutingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateAlertRouting(id, request)));
    }

    @PostMapping @PreAuthorize("hasAuthority('INSTITUTION_WRITE')") @Auditable(action = "CREATE", entityType = "INSTITUTION")
    public ResponseEntity<ApiResponse<InstitutionDto>> create(@Valid @RequestBody InstitutionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(req)));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('INSTITUTION_WRITE')") @Auditable(action = "UPDATE", entityType = "INSTITUTION")
    public ResponseEntity<ApiResponse<InstitutionDto>> update(@PathVariable Long id, @Valid @RequestBody InstitutionCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('INSTITUTION_WRITE')") @Auditable(action = "DELETE", entityType = "INSTITUTION")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.ok(ApiResponse.ok("Institution supprimée."));
    }
}
