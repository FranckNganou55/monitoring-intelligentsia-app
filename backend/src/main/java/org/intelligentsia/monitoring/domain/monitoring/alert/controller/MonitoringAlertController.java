package org.intelligentsia.monitoring.domain.monitoring.alert.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.AlertEventDto;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.CreateAlertRequest;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.MonitoringAlertDto;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.UpdateAlertStatusRequest;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.alert.service.MonitoringAlertService;
import org.intelligentsia.monitoring.domain.monitoring.alert.service.AlertArchiveExportService;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring/alerts")
@RequiredArgsConstructor
public class MonitoringAlertController {

    private final MonitoringAlertService service;
    private final AlertArchiveExportService archiveExportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitoringAlertDto create(
            @Valid @RequestBody CreateAlertRequest request
    ) {
        return service.create(request);
    }

    @GetMapping
    public ApiResponse<PageResponse<MonitoringAlertDto>> list(
            @RequestParam(required = false)
            MonitoringAlert.AlertStatus status,

            @RequestParam(required = false)
            MonitoringAlert.Category category,

            @RequestParam(required = false)
            MonitoringRule.RiskLevel riskLevel,

            @RequestParam(required = false)
            Long ruleId,

            @RequestParam(required = false)
            Long institutionId,

            @RequestParam(required = false)
            Long atmId,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime detectedFrom,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime detectedUntil,

            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer hour,
            @RequestParam(defaultValue = "false") boolean includeArchived,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @AuthenticationPrincipal User user
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "detectedAt"
                )
        );

        // Keep the alerts page aligned with dashboard/fraud institution scope.
        // A terminal-linked alert can have a missing institution_id in older data,
        // so the repository also resolves its institution through the ATM relation.
        Long effectiveInstitutionId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : institutionId;

        return ApiResponse.ok(
                service.findAll(
                        status,
                        category,
                        riskLevel,
                        ruleId,
                        effectiveInstitutionId,
                        atmId,
                        search,
                        detectedFrom,
                        detectedUntil,
                        year,
                        month,
                        day,
                        hour,
                        includeArchived,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    public MonitoringAlertDto findById(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user
    ) {
        MonitoringAlertDto alert = service.findById(id);
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        if (effectiveId != null && !effectiveId.equals(alert.getInstitutionId())) {
            throw new ResourceNotFoundException("Alerte", id);
        }
        return alert;
    }

    @GetMapping("/{id}/events")
    public ApiResponse<List<AlertEventDto>> events(
            @PathVariable Long id,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user
    ) {
        MonitoringAlertDto alert = service.findById(id);
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        if (effectiveId != null && !effectiveId.equals(alert.getInstitutionId())) {
            throw new ResourceNotFoundException("Alerte", id);
        }
        return ApiResponse.ok(
                service.findEvents(id)
        );
    }

    @PatchMapping("/{id}/status")
    public MonitoringAlertDto updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertStatusRequest request,
            @AuthenticationPrincipal User user
    ) {
        return service.updateStatus(
                id,
                request,
                user.getId()
        );
    }

    @GetMapping("/archives/export")
    public org.springframework.http.ResponseEntity<byte[]> exportArchives(
            @RequestParam(defaultValue = "WORD") String format,
            @RequestParam(defaultValue = "false") boolean includeActive,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MonitoringAlert.Category category,
            @RequestParam(required = false) MonitoringRule.RiskLevel riskLevel,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedUntil,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer hour,
            @AuthenticationPrincipal User user
    ) throws java.io.IOException {
        Long effectiveInstitutionId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : institutionId;
        String normalized = format.trim().toUpperCase(java.util.Locale.ROOT);
        byte[] file = archiveExportService.export(normalized, search,
                category, riskLevel, effectiveInstitutionId, detectedFrom, detectedUntil,
                year, month, day, hour, includeActive);
        MediaType type = normalized.equals("PDF") ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType(normalized.equals("EXCEL") ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        String extension = normalized.equals("PDF") ? "pdf" : normalized.equals("EXCEL") ? "xlsx" : "docx";
        return org.springframework.http.ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=archives_alertes." + extension)
                .contentType(type)
                .body(file);
    }

    @PostMapping("/{id}/email")
    public ApiResponse<String> sendConfiguredFraudEmail(
            @PathVariable Long id,
            @RequestParam Long institutionId) {
        service.sendConfiguredFraudEmail(id, institutionId);
        return ApiResponse.ok("Email d’alerte envoyé au destinataire configuré.");
    }
}
