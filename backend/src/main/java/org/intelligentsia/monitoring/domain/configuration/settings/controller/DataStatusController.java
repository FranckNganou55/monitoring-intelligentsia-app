package org.intelligentsia.monitoring.domain.configuration.settings.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DataStatusController {
    private final JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Value("${app.local-configuration.enabled:false}")
    private boolean localConfiguration;
    private final String backendInstance = java.util.UUID.randomUUID().toString();
    @GetMapping("/api/system/data-status")
    public Map<String, Object> status() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("connected", !localConfiguration, "configurationAvailable", true, "backendInstance", backendInstance, "mode", localConfiguration ? "LOCAL_CONFIGURATION" : "CONNECTED");
    }
}
