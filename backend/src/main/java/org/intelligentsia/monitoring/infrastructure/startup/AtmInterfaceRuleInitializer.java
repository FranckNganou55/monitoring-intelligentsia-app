package org.intelligentsia.monitoring.infrastructure.startup;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.domain.monitoring.rule.repository.MonitoringRuleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Splits the legacy rule on databases already migrated through V30. */
@org.springframework.context.annotation.Profile("!local-config")
@Component
@RequiredArgsConstructor
public class AtmInterfaceRuleInitializer implements ApplicationRunner {
    private final MonitoringRuleRepository rules;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional("transactionManager")
    public void run(ApplicationArguments args) {
        // Serialize initialization when several backend instances start together.
        jdbc.execute("SELECT pg_advisory_xact_lock(20261012, 30)");
        MonitoringRule legacy = rules.findFirstByCodeOrderByIdAsc("RULE_ATM_INTERFACE_UNAVAILABLE_5X")
                .orElse(null);
        boolean downCreated = ensureRule("RULE_ATM_INTERFACE_DOWN_5X", "DOWN", 0, 0, legacy);
        boolean signoffCreated = ensureRule("RULE_ATM_INTERFACE_SIGNOFF_5X", "SIGNOFF", 3, 1, legacy);
        if (downCreated || signoffCreated) {
            // The legacy counters could contain a mix of DOWN and SIGNOFF.
            jdbc.update("""
                    UPDATE atm_interface_monitoring_states
                    SET consecutive_status_code = NULL, consecutive_samples = 0,
                        outage_started_at = NULL, alert_raised_for_outage = FALSE,
                        active_alert_id = NULL, last_sample_at = NULL, updated_at = NOW()
                    """);
        }
        if (legacy != null) {
            Long references = jdbc.queryForObject("""
                    SELECT (SELECT COUNT(*) FROM monitoring_alerts WHERE rule_id = ?)
                         + (SELECT COUNT(*) FROM fraud_detections WHERE rule_id = ?)
                    """, Long.class, legacy.getId(), legacy.getId());
            // Only remove an unused legacy row automatically. Historical purges are explicit operations.
            if (references != null && references == 0) rules.delete(legacy);
            else if (Boolean.TRUE.equals(legacy.getEnabled())) {
                legacy.setEnabled(false);
                rules.save(legacy);
            }
        }
    }

    private boolean ensureRule(String code, String label, int status, int order, MonitoringRule legacy) {
        // Existing rules retain the administrator's choices on every restart.
        if (rules.findFirstByCodeOrderByIdAsc(code).isPresent()) return false;
        MonitoringRule rule = new MonitoringRule();
        rule.setCode(code);
        rule.setName("Interface GAB " + label + " (" + status + ")");
        rule.setDescription("Une alerte est créée après le nombre configuré de relevés consécutifs du statut "
                + label + " (" + status + "). Un changement de statut démarre une nouvelle série. "
                + "L’intervalle et le nombre de relevés sont paramétrables dans Administration > Paramètres > Alertes.");
        rule.setCategory(MonitoringRule.RuleCategory.ATM);
        rule.setSourceOrder(order);
        if (legacy != null) {
            rule.setRiskLevel(legacy.getRiskLevel());
            rule.setRiskPercentage(legacy.getRiskPercentage());
            rule.setEnabled(legacy.getEnabled());
            rule.setNotificationEnabled(legacy.getNotificationEnabled());
            rule.setSoundEnabled(legacy.getSoundEnabled());
            rule.setVisualAlertEnabled(legacy.getVisualAlertEnabled());
        }
        rules.save(rule);
        return true;
    }
}
