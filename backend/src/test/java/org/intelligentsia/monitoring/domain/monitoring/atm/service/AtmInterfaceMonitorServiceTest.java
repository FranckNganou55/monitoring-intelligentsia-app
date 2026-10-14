package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import org.intelligentsia.monitoring.domain.monitoring.alert.dto.CreateAlertRequest;
import org.intelligentsia.monitoring.domain.monitoring.alert.dto.MonitoringAlertDto;
import org.intelligentsia.monitoring.domain.monitoring.alert.service.MonitoringAlertService;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.AtmInterfaceMonitoringState;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmInterfaceMonitoringStateRepository;
import org.intelligentsia.monitoring.domain.monitoring.rule.entity.MonitoringRule;
import org.intelligentsia.monitoring.domain.monitoring.rule.repository.MonitoringRuleRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AtmInterfaceMonitorServiceTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    void statusChangeStartsNewSeriesAndRaisesItsOwnAlert(int code) {
        var states = mock(AtmInterfaceMonitoringStateRepository.class);
        var rules = mock(MonitoringRuleRepository.class);
        var alerts = mock(MonitoringAlertService.class);
        var service = new AtmInterfaceMonitorService(null, states, rules, alerts, null, null, null, null);
        var state = new AtmInterfaceMonitoringState();
        state.setIntCode("123");
        state.setConsecutiveStatusCode(code == 0 ? 3 : 0);
        state.setConsecutiveSamples(5);
        state.setAlertRaisedForOutage(true);
        Instant now = Instant.parse("2026-10-12T10:00:00Z");
        state.setLastSampleAt(now.minusSeconds(180));
        when(states.findById("123")).thenReturn(Optional.of(state));
        String label = code == 0 ? "DOWN" : "SIGNOFF";
        String ruleCode = "RULE_ATM_INTERFACE_" + label + "_5X";
        var rule = new MonitoringRule();
        rule.setId(42L);
        when(rules.findFirstByCodeOrderByIdAsc(ruleCode)).thenReturn(Optional.of(rule));
        var created = MonitoringAlertDto.builder().id(99L).build();
        when(alerts.create(any())).thenReturn(created);
        Map<String, Object> row = Map.of("INT_STAT", code, "INT_LABE", "ATM_TEST");
        evaluate(service, row, now);
        assertEquals(1, state.getConsecutiveSamples());
        assertFalse(state.getAlertRaisedForOutage());
        verifyNoInteractions(alerts);
        for (int sample = 1; sample < 5; sample++) evaluate(service, row, now.plusSeconds(sample * 180));
        var request = org.mockito.ArgumentCaptor.forClass(CreateAlertRequest.class);
        verify(alerts).create(request.capture());
        assertEquals(42L, request.getValue().getRuleId());
        assertTrue(request.getValue().getTitle().contains(label + " (" + code + ")"));
        assertTrue(request.getValue().getDescription().contains("Statut : " + code + " — " + label));
        evaluate(service, row, now.plusSeconds(900));
        verify(alerts, times(1)).create(any());
        evaluate(service, Map.of("INT_STAT", 1), now.plusSeconds(1080));
        assertEquals(0, state.getConsecutiveSamples());
        assertFalse(state.getAlertRaisedForOutage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    void usesIndependentIntervalAndThresholdAndCountsOnlyWhenDue(int code) {
        var states = mock(AtmInterfaceMonitoringStateRepository.class);
        var rules = mock(MonitoringRuleRepository.class);
        var alerts = mock(MonitoringAlertService.class);
        var settings = mock(org.intelligentsia.monitoring.domain.configuration.settings.service.AppSettingService.class);
        when(settings.getInteger(anyString(), anyInt())).thenAnswer(call -> switch (call.getArgument(0, String.class)) {
            case "alerts.atm.interface.DOWN.interval.ms" -> 60000;
            case "alerts.atm.interface.DOWN.consecutive.samples" -> 2;
            case "alerts.atm.interface.SIGNOFF.interval.ms" -> 300000;
            case "alerts.atm.interface.SIGNOFF.consecutive.samples" -> 4;
            default -> call.getArgument(1);
        });
        var service = new AtmInterfaceMonitorService(null, states, rules, alerts, null, null, settings, null);
        long[] configuration = ReflectionTestUtils.invokeMethod(service, "configurationFor", code, 180L, 5);
        long interval = code == 0 ? 60 : 300;
        int samples = code == 0 ? 2 : 4;
        assertArrayEquals(new long[]{interval, samples}, configuration);
        var state = new AtmInterfaceMonitoringState(); state.setIntCode("123"); state.setConsecutiveSamples(0); state.setAlertRaisedForOutage(false);
        when(states.findById("123")).thenReturn(Optional.of(state));
        var rule = new MonitoringRule(); rule.setId(42L);
        when(rules.findFirstByCodeOrderByIdAsc(code == 0 ? "RULE_ATM_INTERFACE_DOWN_5X" : "RULE_ATM_INTERFACE_SIGNOFF_5X")).thenReturn(Optional.of(rule));
        when(alerts.create(any())).thenReturn(MonitoringAlertDto.builder().id(99L).build());
        Instant start = Instant.parse("2026-10-13T08:00:00Z");
        var row = Map.<String,Object>of("INT_STAT", code, "INT_LABE", "ATM_TEST");
        for (int sample = 0; sample < samples; sample++) {
            ReflectionTestUtils.invokeMethod(service, "evaluate", row, "123", start.plusSeconds(sample * interval), Map.of(), interval, samples);
            assertEquals(sample + 1, state.getConsecutiveSamples());
            ReflectionTestUtils.invokeMethod(service, "evaluate", row, "123", start.plusSeconds(sample * interval + 1), Map.of(), interval, samples);
            assertEquals(sample + 1, state.getConsecutiveSamples());
            if (sample < samples - 1) verifyNoInteractions(alerts);
        }
        verify(alerts, times(1)).create(any());
        // Recovery is processed immediately even before the failure interval has elapsed.
        ReflectionTestUtils.invokeMethod(service, "evaluate", Map.of("INT_STAT", 4), "123", start.plusSeconds((samples - 1) * interval + 2), Map.of(), interval, samples);
        assertEquals(0, state.getConsecutiveSamples()); assertFalse(state.getAlertRaisedForOutage());
    }

    private void evaluate(AtmInterfaceMonitorService service, Map<String, Object> row, Instant now) {
        ReflectionTestUtils.invokeMethod(service, "evaluate", row, "123", now, Map.of(), 180L, 5);
    }
}
