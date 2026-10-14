package org.intelligentsia.monitoring.domain.monitoring.transaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.transaction.dto.TransactionErrorDto;
import org.intelligentsia.monitoring.domain.monitoring.transaction.dto.TransactionRateDto;
import org.intelligentsia.monitoring.domain.monitoring.transaction.service.TransactionService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Tag(name = "Transactions")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/types") @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<ApiResponse<List<String>>> types(@RequestParam(required = false) Long institutionId, @AuthenticationPrincipal User user) {
        Long effective = user.getScope() == UserScope.INSTITUTION && user.hasInstitution() ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(transactionService.transactionTypes(effective)));
    }

    @GetMapping("/operations") @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<java.util.Map<String, Object>>>> operations(FilterRequest filter,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime until,
            @RequestParam(required = false) String type, @RequestParam(required = false) String search, @AuthenticationPrincipal User user) {
        Long effective = user.getScope() == UserScope.INSTITUTION && user.hasInstitution() ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(transactionService.operations(effective, from, until, type, search, filter.toPageable())));
    }

    @GetMapping("/networks") @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<ApiResponse<List<String>>> networks(@RequestParam(required = false) Long institutionId,
                                                             @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(transactionService.networks(effectiveId)));
    }

    @Operation(summary = "Taux de succès par réseau (historique)")
    @GetMapping("/rates")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<TransactionRateDto>>> rates(
            FilterRequest filter,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(transactionService.findRateHistory(effectiveId, from, to, filter.toPageable(), network, state)));
    }

    @Operation(summary = "Erreurs de transaction (historique)")
    @GetMapping("/errors")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<TransactionErrorDto>>> errors(
            FilterRequest filter,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(transactionService.findErrorHistory(effectiveId, from, to, filter.toPageable(), network, state)));
    }

    @Operation(summary = "Derniers taux (dashboard)")
    @GetMapping("/rates/latest")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<ApiResponse<List<TransactionRateDto>>> latestRates(
            @RequestParam(required = false) Long institutionId,
            @AuthenticationPrincipal User user) {
        Long effectiveId = user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : institutionId;
        return ResponseEntity.ok(ApiResponse.ok(transactionService.findLatestRates(effectiveId)));
    }
}
