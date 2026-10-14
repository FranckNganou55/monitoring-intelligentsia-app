package org.intelligentsia.monitoring.domain.monitoring.dashboard.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.dashboard.dto.DashboardHistoryDto;
import org.intelligentsia.monitoring.domain.monitoring.dashboard.dto.DashboardSummaryDto;
import org.intelligentsia.monitoring.domain.monitoring.dashboard.service.DashboardService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@Tag(name="Dashboard") 
@RestController 
@RequestMapping("/api/dashboard") 
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/summary") 
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> summary(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) Long institutionId
    ) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : institutionId;

        return ResponseEntity.ok(
            ApiResponse.ok(service.getSummary(instId))
        );
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    public ResponseEntity<ApiResponse<DashboardHistoryDto>> history(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long institutionId
    ) {
        Long instId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId()
                : institutionId;

        return ResponseEntity.ok(
                ApiResponse.ok(service.getHistory(instId))
        );
    }
}
