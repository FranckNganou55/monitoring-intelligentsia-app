package org.intelligentsia.monitoring.domain.monitoring.fraud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.intelligentsia.monitoring.domain.monitoring.fraud.dto.FraudDetailDto;
import org.intelligentsia.monitoring.domain.monitoring.fraud.dto.FraudAlertOverviewDto;
import org.intelligentsia.monitoring.domain.monitoring.fraud.dto.FraudRateDto;
import org.intelligentsia.monitoring.domain.monitoring.fraud.service.FraudAlertOverviewService;
import org.intelligentsia.monitoring.domain.monitoring.fraud.service.FraudService;
import org.intelligentsia.monitoring.domain.monitoring.fraud.service.SvfeFraudReadService;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Fraude")
@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService fraudService;
    private final FraudAlertOverviewService fraudAlertOverviewService;
    private final SvfeFraudReadService svfeFraudReadService;

    @Operation(summary = "Vue synthétique des alertes fraude")
    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('FRAUD_READ')")
    public ResponseEntity<ApiResponse<FraudAlertOverviewDto>> alerts(
            @RequestParam(defaultValue = "24") int periodHours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MonitoringRule.RiskLevel riskLevel,
            @RequestParam(required = false) MonitoringAlert.AlertStatus status,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveInstitutionId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(fraudAlertOverviewService.getOverview(
                periodHours, page, size, effectiveInstitutionId, search,
                riskLevel == null ? null : riskLevel.name(),
                status == null ? null : status.name())));
    }

    @Operation(summary = "Taux de fraude")
    @GetMapping("/rates")
    @PreAuthorize("hasAuthority('FRAUD_READ')")
    public ResponseEntity<ApiResponse<PageResponse<FraudRateDto>>> rates(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effInstitutionId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution() ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(fraudService.findRates(filter, effInstitutionId)));
    }

    @Operation(summary = "Détails de fraude")
    @GetMapping("/details")
    @PreAuthorize("hasAuthority('FRAUD_READ')")
    public ResponseEntity<ApiResponse<PageResponse<FraudDetailDto>>> details(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effInstitutionId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution() ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(fraudService.findDetails(filter, effInstitutionId)));
    }

    @Operation(summary = "Fraudes lues directement depuis SVFE")
    @GetMapping("/source")
    @PreAuthorize("hasAuthority('FRAUD_READ')")
    public ResponseEntity<ApiResponse<PageResponse<FraudDetailDto>>> source(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveInstitutionId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution() ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(svfeFraudReadService.find(filter, effectiveInstitutionId)));
    }
}
