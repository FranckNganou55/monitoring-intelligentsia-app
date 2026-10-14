package org.intelligentsia.monitoring.domain.monitoring.fleet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.fleet.dto.CardOverLimitDto;
import org.intelligentsia.monitoring.domain.monitoring.fleet.dto.FleetAccountDto;
import org.intelligentsia.monitoring.domain.monitoring.fleet.service.FleetService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Flotte")
@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    @Operation(summary = "Comptes flotte")
    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('FLEET_READ')")
    public ResponseEntity<ApiResponse<PageResponse<FleetAccountDto>>> accounts(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(fleetService.findAccounts(filter, effId)));
    }

    @Operation(summary = "Cartes dépassant la limite")
    @GetMapping("/cards-over-limit")
    @PreAuthorize("hasAuthority('FLEET_READ')")
    public ResponseEntity<ApiResponse<PageResponse<CardOverLimitDto>>> cardsOverLimit(
            FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(fleetService.findCardsOverLimit(filter, effId)));
    }
}
