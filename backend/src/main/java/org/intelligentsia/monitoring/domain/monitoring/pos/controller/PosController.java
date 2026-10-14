package org.intelligentsia.monitoring.domain.monitoring.pos.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.PosCreateRequest;
import org.intelligentsia.monitoring.domain.monitoring.pos.dto.PosTerminalDto;
import org.intelligentsia.monitoring.domain.monitoring.pos.service.PosService;
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

@Tag(name = "Terminaux POS") @RestController @RequestMapping("/api/pos") @RequiredArgsConstructor
public class PosController {
    private final PosService service;

    @GetMapping @PreAuthorize("hasAuthority('POS_READ')")
    public ResponseEntity<ApiResponse<PageResponse<PosTerminalDto>>> list(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(filter, instId)));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('POS_READ')")
    public ResponseEntity<ApiResponse<PosTerminalDto>> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        PosTerminalDto terminal = service.findById(id);
        if (effectiveId != null && !effectiveId.equals(terminal.getInstitutionId())) {
            throw new ResourceNotFoundException("Terminal POS", id);
        }
        return ResponseEntity.ok(ApiResponse.ok(terminal));
    }

    @PostMapping @PreAuthorize("hasAuthority('POS_WRITE')") @Auditable(action = "CREATE", entityType = "POS_TERMINAL")
    public ResponseEntity<ApiResponse<PosTerminalDto>> create(@Valid @RequestBody PosCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(req)));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('POS_WRITE')") @Auditable(action = "UPDATE", entityType = "POS_TERMINAL")
    public ResponseEntity<ApiResponse<PosTerminalDto>> update(@PathVariable Long id, @Valid @RequestBody PosCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('POS_WRITE')") @Auditable(action = "DELETE", entityType = "POS_TERMINAL")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.ok(ApiResponse.<Void>ok("Terminal POS supprimé."));
    }
}
