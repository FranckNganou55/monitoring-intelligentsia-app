package org.intelligentsia.monitoring.domain.monitoring.rule.service;

import org.intelligentsia.monitoring.domain.monitoring.rule.dto.MonitoringRuleDto;
import org.intelligentsia.monitoring.domain.monitoring.rule.dto.MonitoringRuleRequest;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.domain.monitoring.rule.repository.MonitoringRuleRepository;
import org.intelligentsia.monitoring.domain.monitoring.alert.repository.MonitoringAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class MonitoringRuleService {

    private final MonitoringRuleRepository repository;
    private final MonitoringAlertRepository alertRepository;

    @Transactional(readOnly = true)
    public List<MonitoringRuleDto> findAll() {
        return repository.findAllInDisplayOrder()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonitoringRuleDto> findByCategory(
            MonitoringRule.RuleCategory category
    ) {
        return repository
                .findByCategoryInDisplayOrder(category)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitoringRuleDto findById(Long id) {
        return toDto(getRule(id));
    }

    public MonitoringRuleDto create(MonitoringRuleRequest request) {
        MonitoringRule rule = new MonitoringRule();

        applyRequest(rule, request);

        return toDto(repository.save(rule));
    }

    public MonitoringRuleDto update(
            Long id,
            MonitoringRuleRequest request
    ) {
        MonitoringRule rule = getRule(id);
        applyRequest(rule, request);

        return toDto(repository.save(rule));
    }

    public MonitoringRuleDto updateRiskLevel(
            Long id,
            MonitoringRule.RiskLevel riskLevel
    ) {
        MonitoringRule rule = getRule(id);

        rule.setRiskLevel(riskLevel);
        rule.setRiskPercentage(riskPercentageFor(riskLevel));

        return toDto(repository.save(rule));
    }

    public MonitoringRuleDto updateEnabled(
            Long id,
            boolean enabled
    ) {
        MonitoringRule rule = getRule(id);

        rule.setEnabled(enabled);

        return toDto(repository.save(rule));
    }

    public void delete(Long id) {
        MonitoringRule rule = getRule(id);

        repository.delete(rule);
    }

    private MonitoringRule getRule(Long id) {
        return repository.findById(id)
                .filter(rule -> !"RULE_ATM_INTERFACE_UNAVAILABLE_5X".equals(rule.getCode()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Règle introuvable."
                ));
    }

    private void applyRequest(
            MonitoringRule rule,
            MonitoringRuleRequest request
    ) {
        if ("RULE_ATM_INTERFACE_UNAVAILABLE_5X".equals(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette règle a été remplacée par les règles DOWN et SIGNOFF.");
        }
        rule.setCode(request.getCode());
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setCategory(request.getCategory());
        rule.setRiskLevel(request.getRiskLevel());
        // Risk percentage is determined by the selected level; ignore client supplied values.
        rule.setRiskPercentage(riskPercentageFor(request.getRiskLevel()));

        rule.setEnabled(
                request.getEnabled() == null
                        || request.getEnabled()
        );

        rule.setNotificationEnabled(
                request.getNotificationEnabled() == null
                        || request.getNotificationEnabled()
        );

        rule.setSoundEnabled(
                request.getSoundEnabled() != null
                        && request.getSoundEnabled()
        );

        rule.setVisualAlertEnabled(
                request.getVisualAlertEnabled() != null
                        && request.getVisualAlertEnabled()
        );
    }

    private MonitoringRuleDto toDto(MonitoringRule rule) {
        long alertCount = alertRepository.countByRuleId(rule.getId());
        long totalAlerts = alertRepository.count();
        double alertPercentage = totalAlerts == 0
                ? 0.0
                : alertCount * 100.0 / totalAlerts;

        return MonitoringRuleDto.builder()
                .id(rule.getId())
                .code(rule.getCode())
                .name(rule.getName())
                .description(rule.getDescription())
                .category(rule.getCategory())
                .riskLevel(rule.getRiskLevel())
                .riskPercentage(riskPercentageFor(rule.getRiskLevel()).doubleValue())
                .enabled(rule.getEnabled())
                .notificationEnabled(rule.getNotificationEnabled())
                .soundEnabled(rule.getSoundEnabled())
                .visualAlertEnabled(rule.getVisualAlertEnabled())
                .alertCount(alertCount)
                .alertPercentage(alertPercentage)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private BigDecimal riskPercentageFor(MonitoringRule.RiskLevel level) {
        return switch (level) {
            case TYPIQUE -> BigDecimal.valueOf(25);
            case MOINS_CRITIQUE -> BigDecimal.valueOf(50);
            case MOYEN -> BigDecimal.valueOf(75);
            case CRITIQUE -> BigDecimal.valueOf(100);
        };
    }
}
