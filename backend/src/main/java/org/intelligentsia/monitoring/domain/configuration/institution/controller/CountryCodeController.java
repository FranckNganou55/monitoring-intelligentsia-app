package org.intelligentsia.monitoring.domain.configuration.institution.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.configuration.institution.dto.CreateCountryCodeRequest;
import org.intelligentsia.monitoring.domain.configuration.institution.service.CountryCodeService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings/country-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('INSTITUTION_WRITE')")
public class CountryCodeController {
    private final CountryCodeService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody CreateCountryCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(request)));
    }
}
