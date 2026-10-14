package org.intelligentsia.monitoring.domain.extraction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionTemplateDto;
import org.intelligentsia.monitoring.domain.extraction.dto.ExtractionTemplateRequest;
import org.intelligentsia.monitoring.domain.extraction.service.ExtractionTemplateService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/extraction/templates")
@RequiredArgsConstructor
public class ExtractionTemplateController {

    private final ExtractionTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<ExtractionTemplateDto>>> list(FilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.findAll(filter)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<List<ExtractionTemplateDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(templateService.findAllActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXTRACTION_READ')")
    public ResponseEntity<ApiResponse<ExtractionTemplateDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXTRACTION_WRITE')")
    public ResponseEntity<ApiResponse<ExtractionTemplateDto>> create(
            @Valid @RequestBody ExtractionTemplateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(templateService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXTRACTION_WRITE')")
    public ResponseEntity<ApiResponse<ExtractionTemplateDto>> update(
            @PathVariable Long id, @Valid @RequestBody ExtractionTemplateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXTRACTION_WRITE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Template supprimé."));
    }
}
