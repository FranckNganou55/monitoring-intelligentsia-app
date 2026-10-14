package org.intelligentsia.monitoring.domain.audit.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.audit.dto.AuditLogDto;
import org.intelligentsia.monitoring.domain.audit.service.AuditService;
import org.intelligentsia.monitoring.shared.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@Tag(name="Audit") @RestController @RequestMapping("/api/audit") @RequiredArgsConstructor
public class AuditController {
    private final AuditService service;
    @GetMapping @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> list(
            @RequestParam(required=false) Long userId,
            @RequestParam(required=false) String entityType,
            @RequestParam(required=false) String action,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(userId, entityType, action, page, size)));
    }
}
