package org.intelligentsia.monitoring.domain.monitoring.rule.controller;

import org.intelligentsia.monitoring.domain.monitoring.rule.dto.MonitoringRuleDto;
import org.intelligentsia.monitoring.domain.monitoring.rule.dto.MonitoringRuleRequest;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.domain.monitoring.rule.service.MonitoringRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring/rules")
@RequiredArgsConstructor
public class MonitoringRuleController {

    private final MonitoringRuleService service;

    @GetMapping
    public List<MonitoringRuleDto> findAll(
            @RequestParam(required = false)
            MonitoringRule.RuleCategory category
    ) {
        if (category == null) {
            return service.findAll();
        }

        return service.findByCategory(category);
    }

    @GetMapping("/{id}")
    public MonitoringRuleDto findById(
            @PathVariable Long id
    ) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitoringRuleDto create(
            @Valid @RequestBody MonitoringRuleRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MonitoringRuleDto update(
            @PathVariable Long id,
            @Valid @RequestBody MonitoringRuleRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/risk-level")
    public MonitoringRuleDto updateRiskLevel(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        MonitoringRule.RiskLevel riskLevel =
                MonitoringRule.RiskLevel.valueOf(
                        body.get("riskLevel")
                );

        return service.updateRiskLevel(id, riskLevel);
    }

    @PatchMapping("/{id}/enabled")
    public MonitoringRuleDto updateEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean enabled = body.get("enabled");

        return service.updateEnabled(
                id,
                enabled != null && enabled
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {
        service.delete(id);
    }
}