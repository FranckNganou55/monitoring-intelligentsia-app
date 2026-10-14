package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.extraction.service.ExtractionSchedulerService;
import org.intelligentsia.monitoring.infrastructure.scheduler.MonitoringScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@EnabledIfSystemProperty(named = "monitoring.dbTests", matches = "true")
@SpringBootTest(properties = {"spring.datasource.url=jdbc:postgresql://localhost:5432/monitoring_test", "app.svfe.enabled=false"})
@AutoConfigureMockMvc
class MonitoringEndpointsTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired org.intelligentsia.monitoring.domain.configuration.settings.service.MailConfigService mailConfig;
    @MockitoBean MonitoringScheduler scheduler;
    @MockitoBean ExtractionSchedulerService extractionScheduler;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void changingDownConfigurationDoesNotChangeSignoffConfiguration() throws Exception {
        var principal = new User(); principal.setId(1L); principal.setScope(UserScope.ADMIN);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var signoffBefore = jdbc.queryForList("SELECT cle,valeur FROM app_settings WHERE cle LIKE 'alerts.atm.interface.SIGNOFF.%' ORDER BY cle");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/settings/atm-interface/DOWN")
                .contentType("application/json").content("{\"samples\":2,\"intervalMs\":60000}").with(auth)).andExpect(status().isOk());
        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals("2", jdbc.queryForObject("SELECT valeur FROM app_settings WHERE cle='alerts.atm.interface.DOWN.consecutive.samples'", String.class));
        org.junit.jupiter.api.Assertions.assertEquals("60000", jdbc.queryForObject("SELECT valeur FROM app_settings WHERE cle='alerts.atm.interface.DOWN.interval.ms'", String.class));
        org.junit.jupiter.api.Assertions.assertEquals(signoffBefore, jdbc.queryForList("SELECT cle,valeur FROM app_settings WHERE cle LIKE 'alerts.atm.interface.SIGNOFF.%' ORDER BY cle"));
    }

    @Test
    void transactionDetailsTypesAndIndependentSettingsUseDatabaseAndRespectScope() throws Exception {
        var principal = new User(); principal.setId(1L); principal.setScope(UserScope.ADMIN);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                java.util.List.of(new SimpleGrantedAuthority("TRANSACTION_READ"), new SimpleGrantedAuthority("ROLE_ADMIN"))));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM fraud_detections", Long.class);
        mvc.perform(get("/api/transactions/operations").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(total.intValue()));
        mvc.perform(get("/api/transactions/types").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        mvc.perform(get("/api/transactions/operations").param("type", "NO_SUCH_TYPE").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/transactions/operations").param("institutionId", "-1").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/transactions/operations").param("from", "2026-10-13T00:00:00").param("until", "2026-10-12T00:00:00").with(auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/system/data-status").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.connected").value(true));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/settings/atm-interface/DOWN")
                .contentType("application/json").content("{\"samples\":0,\"intervalMs\":60000}").with(auth)).andExpect(status().isUnprocessableEntity());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/settings/atm-interface/SIGNOFF")
                .contentType("application/json").content("{\"samples\":3,\"intervalMs\":61000}").with(auth)).andExpect(status().isBadRequest());
        for (String code : new String[]{"DOWN", "SIGNOFF"}) {
            org.junit.jupiter.api.Assertions.assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM app_settings WHERE cle LIKE ?", Integer.class, "alerts.atm.interface." + code + ".%"));
        }
        principal.setScope(UserScope.INSTITUTION);
        var institution = new org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution(); institution.setId(-1L); principal.setInstitution(institution);
        mvc.perform(get("/api/transactions/operations").param("institutionId", "1").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/transactions/types").param("institutionId", "1").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void archiveScopeAndPeriodExportsPreserveAllMatchingAlerts() throws Exception {
        var principal = new User(); principal.setId(1L); principal.setScope(UserScope.ADMIN);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var directory = java.nio.file.Path.of("tmp", "export-review");
        java.nio.file.Files.createDirectories(directory);
        for (boolean all : new boolean[]{false, true}) {
            byte[] bytes = mvc.perform(get("/api/monitoring/alerts/archives/export").param("format", "PDF")
                    .param("includeActive", String.valueOf(all)).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
                String text = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
                var matcher = java.util.regex.Pattern.compile("Alerte #(\\d+)").matcher(text);
                java.util.Set<Long> ids = new java.util.HashSet<>();
                while (matcher.find()) ids.add(Long.parseLong(matcher.group(1)));
                var expected = jdbc.queryForList("SELECT id FROM monitoring_alerts WHERE deleted_at IS NULL" + (all ? "" : " AND status='ARCHIVED'"), Long.class);
                org.junit.jupiter.api.Assertions.assertEquals(new java.util.HashSet<>(expected), ids);
                for (var page : pdf.getPages()) org.junit.jupiter.api.Assertions.assertTrue(page.getMediaBox().getHeight() > page.getMediaBox().getWidth());
            }
            if (all) java.nio.file.Files.write(directory.resolve("archives.pdf"), bytes);
        }
        for (String format : new String[]{"WORD", "EXCEL"}) {
            byte[] bytes = mvc.perform(get("/api/monitoring/alerts/archives/export").param("format", format).param("includeActive", "true").with(auth))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            java.nio.file.Files.write(directory.resolve("archives." + (format.equals("WORD") ? "docx" : "xlsx")), bytes);
        }
        var before = jdbc.queryForList("SELECT id, report_date, total_detected, generated_at FROM fraud_daily_reports ORDER BY id");
        for (String format : new String[]{"PDF", "WORD", "EXCEL"}) {
            byte[] bytes = mvc.perform(get("/api/fraud/reports/daily/export-period").param("fromDate", "2026-09-28").param("toDate", "2026-10-12")
                    .param("comment", "Observations de la période : contrôle des alertes.").param("format", format).with(auth))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            if (format.equals("PDF")) try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
                String text = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
                var matcher = java.util.regex.Pattern.compile("Alerte #(\\d+)").matcher(text);
                java.util.Set<Long> ids = new java.util.HashSet<>();
                while (matcher.find()) ids.add(Long.parseLong(matcher.group(1)));
                var expected = jdbc.queryForList("SELECT id FROM monitoring_alerts WHERE category='FRAUD' AND deleted_at IS NULL AND detected_at >= '2026-09-28' AND detected_at < '2026-10-13'", Long.class);
                org.junit.jupiter.api.Assertions.assertEquals(new java.util.HashSet<>(expected), ids);
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("28/09/2026 00:00:00"));
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("12/10/2026 23:59:59"));
            }
            java.nio.file.Files.write(directory.resolve("periode." + (format.equals("PDF") ? "pdf" : format.equals("WORD") ? "docx" : "xlsx")), bytes);
        }
        org.junit.jupiter.api.Assertions.assertEquals(before, jdbc.queryForList("SELECT id, report_date, total_detected, generated_at FROM fraud_daily_reports ORDER BY id"));
        mvc.perform(get("/api/fraud/reports/daily/export-period").param("fromDate", "2026-10-12").param("toDate", "2026-09-28").with(auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fraud/reports/daily").param("fromDate", "2090-01-01").param("toDate", "2090-01-02").with(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        principal.setScope(UserScope.INSTITUTION);
        var institution = new org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution(); institution.setId(1L); principal.setInstitution(institution);
        mvc.perform(get("/api/fraud/reports/daily/export-period").param("fromDate", "2026-09-28").param("toDate", "2026-10-12").with(auth)).andExpect(status().isConflict());
    }

    @Test
    @EnabledIfSystemProperty(named = "monitoring.smtpTest", matches = "true")
    void authenticatesUsingDatabaseSmtpSettingsWithoutSendingMail() throws Exception {
        ((org.springframework.mail.javamail.JavaMailSenderImpl) mailConfig.buildSender()).testConnection();
    }

    @Test
    void rolesAreVisibleWithPermissionAndDeniedWithoutIt() throws Exception {
        Integer grant = jdbc.queryForObject("""
                SELECT COUNT(*) FROM role_permissions rp JOIN roles r ON r.id=rp.role_id
                JOIN permissions p ON p.id=rp.permission_id WHERE r.code='ROLE_ADMIN' AND p.code='ROLE_READ'
                """, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, grant);
        var reader = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_READ"))));
        mvc.perform(get("/api/roles").with(reader)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.code == 'ROLE_ADMIN')].permissions[0]").exists());
        var nonReader = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test", null,
                java.util.List.of(new SimpleGrantedAuthority("ATM_READ"))));
        mvc.perform(get("/api/roles").with(nonReader)).andExpect(status().isForbidden());
    }

    @Test
    void retiredCombinedRuleIsRemovedFromCatalogueWhileKeepingItsHistory() throws Exception {
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test", null,
                java.util.List.of(new SimpleGrantedAuthority("MONITORING_RULE_VIEW"))));
        for (String category : new String[]{"", "ATM"}) {
            var response = mvc.perform(get("/api/monitoring/rules").param("category", category).with(auth))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertFalse(response.contains("RULE_ATM_INTERFACE_UNAVAILABLE_5X"));
            org.junit.jupiter.api.Assertions.assertTrue(response.contains("RULE_ATM_INTERFACE_DOWN_5X"));
            org.junit.jupiter.api.Assertions.assertTrue(response.contains("RULE_ATM_INTERFACE_SIGNOFF_5X"));
        }
        var legacy = jdbc.queryForList("SELECT id FROM monitoring_rules WHERE code='RULE_ATM_INTERFACE_UNAVAILABLE_5X'", Long.class);
        if (!legacy.isEmpty()) {
            mvc.perform(get("/api/monitoring/rules/{id}", legacy.get(0)).with(auth)).andExpect(status().isNotFound());
        }
    }

    @Test
    void servesMovementsExportsAndNetworkFiltersOnTheRealSchema() throws Exception {
        var principal = new User(); principal.setId(1L); principal.setLogin("test"); principal.setScope(UserScope.ADMIN);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                java.util.List.of(new SimpleGrantedAuthority("ATM_READ"), new SimpleGrantedAuthority("TRANSACTION_READ"), new SimpleGrantedAuthority("NETWORK_READ"))));
        Long id = jdbc.queryForObject("SELECT MIN(id) FROM atms", Long.class);
        mvc.perform(get("/api/atms/{id}/movements", id).with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
        mvc.perform(get("/api/atms/{id}/movements", id).param("status", "DOWN").with(auth)).andExpect(status().isOk());
        mvc.perform(get("/api/atms/{id}/movements", id).param("institutionId", "-1").with(auth)).andExpect(status().isNotFound());
        for (String format : new String[]{"PDF", "WORD", "EXCEL"}) {
            mvc.perform(get("/api/atms/{id}/movements/export", id).param("format", format).with(auth))
                    .andExpect(status().isOk()).andExpect(header().exists("Content-Disposition"));
            mvc.perform(get("/api/atms/report").param("format", format).param("search", "TEST").with(auth)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/atms/report").param("format", "INVALID").with(auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/network/latest").with(auth)).andExpect(status().isOk());
        mvc.perform(get("/api/transactions/networks").with(auth)).andExpect(status().isOk());
        for (String endpoint : new String[]{"rates", "errors"}) {
            mvc.perform(get("/api/transactions/" + endpoint).with(auth)).andExpect(status().isOk());
            mvc.perform(get("/api/transactions/" + endpoint).param("network", "NO_SUCH_NETWORK").param("state", "UP").with(auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
            mvc.perform(get("/api/transactions/" + endpoint).param("institutionId", "-1").with(auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Test void cassetteApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/gabs/cassettes")).andExpect(status().isUnauthorized());
    }
    @Test void cassetteApiRequiresAtmReadPermission() throws Exception {
        User user = new User(); user.setId(-1L); user.setScope(UserScope.GLOBAL);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        mvc.perform(get("/api/gabs/cassettes").with(auth)).andExpect(status().isForbidden());
    }
    @Test void disabledOracleIsReportedByCassetteApi() throws Exception {
        User user = new User(); user.setId(-1L); user.setScope(UserScope.GLOBAL);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, java.util.List.of(new SimpleGrantedAuthority("ATM_READ"))));
        mvc.perform(get("/api/gabs/cassettes").with(auth)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ORACLE_CASSETTES_UNAVAILABLE"));
    }
    @Test void cassetteCorsPreflightAllowsFrontendAndRejectsOtherOrigins() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/gabs/cassettes")
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/gabs/cassettes")
                .header("Origin", "https://untrusted.invalid").header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void explicitExcelFixtureIsVisibleOnlyInItsInstitutionAndDeletesWithGab() throws Exception {
        Long institutionId = jdbc.queryForObject("SELECT id FROM institutions ORDER BY id LIMIT 1", Long.class);
        Long atmId = jdbc.queryForObject("INSERT INTO atms(institution_id,terminal_id) VALUES (?,?) RETURNING id", Long.class,
                institutionId, "TEST-CASSETTE-" + java.util.UUID.randomUUID());
        String payload = """
                {"codeGab":1067780,"numeroGab":"108401001","nomGab":"GAB Excel",
                 "source":"TEST_EXCEL","cassettes":[{"cassetteNum":1,"devise":"28",
                 "valeurBillet":100,"billetsRestants":null,"billetsRejetes":0,
                 "billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2025-06-21T08:44:35"}]}
                """;
        jdbc.update("INSERT INTO atm_test_cassette_fixtures(atm_id,dataset_code,payload) VALUES (?, 'JUNIT_EXCEL', ?::jsonb)", atmId, payload);
        var institution = new org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution();
        institution.setId(institutionId);
        User reader = new User(); reader.setId(-1L); reader.setScope(UserScope.INSTITUTION); reader.setInstitution(institution);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(reader, null,
                java.util.List.of(new SimpleGrantedAuthority("ATM_READ"))));
        mvc.perform(get("/api/gabs/cassettes").param("atmId", atmId.toString()).param("institutionId", "-1").with(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].source").value("TEST_EXCEL"))
                .andExpect(jsonPath("$.data[0].cassettes[0].billetsRestants").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data[0].cassettes[0].billetsRejetes").value(0))
                .andExpect(jsonPath("$.data[0].cassettes[0].derniereMaj").value("2025-06-21T08:44:35"));
        institution.setId(-1L);
        mvc.perform(get("/api/gabs/cassettes").param("atmId", atmId.toString()).with(auth)).andExpect(status().isNotFound());
        jdbc.update("DELETE FROM atms WHERE id=?", atmId);
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM atm_test_cassette_fixtures WHERE atm_id=?", Integer.class, atmId));
    }

    @Test @org.springframework.transaction.annotation.Transactional
    void interfaceStatusOverridesTerminalAndUsesTheFourRequestedCategories() throws Exception {
        Long institutionId=jdbc.queryForObject("SELECT id FROM institutions ORDER BY id LIMIT 1", Long.class);
        String terminal="TEST-INTERFACE-" + java.util.UUID.randomUUID().toString().substring(0,8);
        Long atmId=jdbc.queryForObject("INSERT INTO atms(institution_id,terminal_id) VALUES (?,?) RETURNING id", Long.class,institutionId,terminal);
        jdbc.update("INSERT INTO atm_snapshots(atm_id,snapshot_time,status_id,all_ok) VALUES (?,NOW(),'4',TRUE)",atmId);
        Long interfaceId=jdbc.queryForObject("INSERT INTO atm_interface_snapshots(int_code,int_iden,int_labe,int_stat) VALUES ('JUNIT',?,?,0) RETURNING id",Long.class,"ATM_"+terminal,"ATM_"+terminal);
        User reader=new User(); reader.setId(-1L); reader.setScope(UserScope.GLOBAL);
        var auth=authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(reader,null,java.util.List.of(new SimpleGrantedAuthority("ATM_READ"))));
        for (var item : java.util.Map.of(0,"error",1,"warning",3,"warning",4,"ok",9,"unknown").entrySet()) {
            jdbc.update("UPDATE atm_interface_snapshots SET int_stat=? WHERE id=?",item.getKey(),interfaceId);
            String expected=item.getKey()==9 ? "UNKNOWN" : item.getKey().toString();
            mvc.perform(get("/api/atms").param("search",terminal).param("status",item.getValue()).with(auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].statusId").value(expected));
            mvc.perform(get("/api/atms/"+atmId+"/snapshots/latest").with(auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.statusId").value(expected));
        }
    }
}
