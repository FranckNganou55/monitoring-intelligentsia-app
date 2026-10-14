package org.intelligentsia.monitoring.domain.monitoring.fraud.report.controller;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.fraud.report.dto.FraudDailyReportDto;
import org.intelligentsia.monitoring.domain.monitoring.fraud.report.service.FraudDailyReportService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/fraud/reports/daily")
@RequiredArgsConstructor
public class FraudDailyReportController {
    private final FraudDailyReportService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('FRAUD_REPORT_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<FraudDailyReportDto>>> list(
            FilterRequest filter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = effectiveInstitutionId(user, institutionId);
        // Daily report rows are currently unique by date and have no institution key.
        // Return no cross-institution rows until the report source can be scoped safely.
        if (effectiveId != null) {
            return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(org.springframework.data.domain.Page.empty(filter.toPageable()))));
        }
        return ResponseEntity.ok(ApiResponse.ok(service.list(filter, fromDate, toDate)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FRAUD_REPORT_GENERATE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<FraudDailyReportDto>> generate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "WORD") String format,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime untilTime,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        if (effectiveInstitutionId(user, institutionId) != null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Les rapports journaliers ne sont pas encore rattachés à une institution dans la base actuelle.");
        }
        return ResponseEntity.ok(ApiResponse.ok(service.generate(date, format, comment, fromTime, untilTime)));
    }

    @GetMapping("/export-period")
    @PreAuthorize("hasAnyAuthority('FRAUD_REPORT_GENERATE', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime untilTime,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) throws java.io.IOException {
        if (effectiveInstitutionId(user, institutionId) != null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Les rapports ne sont pas encore rattachés à une institution.");
        byte[] file = service.exportPeriod(fromDate, toDate, format, comment, fromTime, untilTime);
        String normalized = format.trim().toUpperCase(java.util.Locale.ROOT);
        String extension = normalized.equals("PDF") ? "pdf" : normalized.equals("EXCEL") ? "xlsx" : "docx";
        MediaType type = normalized.equals("PDF") ? MediaType.APPLICATION_PDF : MediaType.parseMediaType(normalized.equals("EXCEL")
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=rapport_fraude_" + fromDate + "_" + toDate + "." + extension).contentType(type).body(file);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('FRAUD_REPORT_DOWNLOAD', 'ROLE_ADMIN')")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @RequestParam(defaultValue = "WORD") String format,
                                             @RequestParam(required = false) Long institutionId,
                                             @AuthenticationPrincipal User user) {
        if (effectiveInstitutionId(user, institutionId) != null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Document introuvable.");
        }
        var file = service.getDocument(id, format);
        MediaType contentType = switch (format.toUpperCase()) {
            case "PDF" -> MediaType.APPLICATION_PDF;
            case "EXCEL" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        };
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(contentType)
                .body(new PathResource(file));
    }

    private Long effectiveInstitutionId(User user, Long institutionId) {
        return user != null && user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
    }
}
