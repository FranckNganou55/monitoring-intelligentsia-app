package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsRule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsRuleRepository extends JpaRepository<CmsRule, String> {

    @Query(value = "SELECT id ruleId, descr ruleDescription, NULL ruleStatus"
            + " FROM ((SELECT RUL_IDEN id, RUL_LABE descr FROM oif_rules)"
            + " UNION"
            + " (SELECT RULE_CODE id, RULE_DESCRIPTION descr FROM RISK_RULES))"
            + " WHERE id = :ruleId", nativeQuery = true)
    CmsRule findRule(@Param("ruleId") String ruleId);

    @Query(value = "SELECT id ruleId, descr ruleDescription, NULL ruleStatus"
            + " FROM ((SELECT RUL_IDEN id, RUL_LABE descr FROM oif_rules)"
            + " UNION"
            + " (SELECT RULE_CODE id, RULE_DESCRIPTION descr FROM RISK_RULES))",
            nativeQuery = true)
    List<CmsRule> findAllRules();
}
