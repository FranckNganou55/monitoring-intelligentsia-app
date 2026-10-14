package org.intelligentsia.monitoring.infrastructure.startup;

import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.domain.monitoring.rule.repository.MonitoringRuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AtmInterfaceRuleInitializerTest {
    @Test
    void removesUnusedLegacyRuleAfterItsReplacementsExist() {
        var repository = mock(MonitoringRuleRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        var legacy = new MonitoringRule(); legacy.setId(44L);
        legacy.setCode("RULE_ATM_INTERFACE_UNAVAILABLE_5X");
        when(repository.findFirstByCodeOrderByIdAsc("RULE_ATM_INTERFACE_UNAVAILABLE_5X")).thenReturn(Optional.of(legacy));
        when(repository.findFirstByCodeOrderByIdAsc("RULE_ATM_INTERFACE_DOWN_5X")).thenReturn(Optional.of(new MonitoringRule()));
        when(repository.findFirstByCodeOrderByIdAsc("RULE_ATM_INTERFACE_SIGNOFF_5X")).thenReturn(Optional.of(new MonitoringRule()));
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(44L), eq(44L))).thenReturn(0L);
        new AtmInterfaceRuleInitializer(repository, jdbc).run(null);
        verify(repository).delete(legacy);
        verify(repository, never()).save(any());
    }

    @Test
    void splitsOnceAndPreservesIndependentSettingsOnRestart() {
        var repository = mock(MonitoringRuleRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        var stored = new HashMap<String, MonitoringRule>();
        var saved = new ArrayList<MonitoringRule>();
        var legacy = new MonitoringRule();
        legacy.setCode("RULE_ATM_INTERFACE_UNAVAILABLE_5X");
        legacy.setRiskLevel(MonitoringRule.RiskLevel.CRITIQUE);
        legacy.setSoundEnabled(true);
        legacy.setId(44L);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(44L), eq(44L))).thenReturn(10L);
        stored.put(legacy.getCode(), legacy);
        when(repository.findFirstByCodeOrderByIdAsc(anyString()))
                .thenAnswer(call -> Optional.ofNullable(stored.get(call.getArgument(0))));
        when(repository.save(any())).thenAnswer(call -> {
            MonitoringRule rule = call.getArgument(0);
            stored.put(rule.getCode(), rule);
            saved.add(rule);
            return rule;
        });
        var initializer = new AtmInterfaceRuleInitializer(repository, jdbc);
        initializer.run(null);
        assertFalse(legacy.getEnabled());
        var down = stored.get("RULE_ATM_INTERFACE_DOWN_5X");
        var signoff = stored.get("RULE_ATM_INTERFACE_SIGNOFF_5X");
        assertTrue(down.getEnabled());
        assertTrue(signoff.getEnabled());
        assertEquals(MonitoringRule.RiskLevel.CRITIQUE, down.getRiskLevel());
        assertTrue(signoff.getSoundEnabled());
        assertEquals(3, saved.size());
        down.setEnabled(false);
        signoff.setRiskLevel(MonitoringRule.RiskLevel.TYPIQUE);
        initializer.run(null);
        assertEquals(3, saved.size());
        assertFalse(down.getEnabled());
        assertEquals(MonitoringRule.RiskLevel.TYPIQUE, signoff.getRiskLevel());
        verify(jdbc, times(1)).update(anyString());
    }
}
