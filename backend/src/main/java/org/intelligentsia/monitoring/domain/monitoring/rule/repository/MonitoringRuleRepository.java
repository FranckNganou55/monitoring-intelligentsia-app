package org.intelligentsia.monitoring.domain.monitoring.rule.repository;

import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MonitoringRuleRepository
        extends JpaRepository<MonitoringRule, Long> {

    @Query("""
            SELECT rule FROM MonitoringRule rule
            WHERE rule.category = :category AND rule.code <> 'RULE_ATM_INTERFACE_UNAVAILABLE_5X'
            ORDER BY CASE WHEN rule.sourceOrder IS NULL THEN 1 ELSE 0 END,
                     rule.sourceOrder, rule.riskLevel, rule.name, rule.id
            """)
    List<MonitoringRule> findByCategoryInDisplayOrder(
            @Param("category") MonitoringRule.RuleCategory category);

    @Query("""
            SELECT rule FROM MonitoringRule rule
            WHERE rule.code <> 'RULE_ATM_INTERFACE_UNAVAILABLE_5X'
            ORDER BY CASE WHEN rule.sourceOrder IS NULL THEN 1 ELSE 0 END,
                     rule.sourceOrder, rule.riskLevel, rule.name, rule.id
            """)
    List<MonitoringRule> findAllInDisplayOrder();

    Optional<MonitoringRule> findFirstByCodeOrderByIdAsc(String code);
}
