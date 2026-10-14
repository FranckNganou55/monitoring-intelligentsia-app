package org.intelligentsia.monitoring.infrastructure.local;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.intelligentsia.monitoring.domain.auth.entity.*;
import org.intelligentsia.monitoring.domain.auth.repository.*;
import org.intelligentsia.monitoring.domain.configuration.settings.service.MailConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@SpringBootTest
@ActiveProfiles("local-config")
@AutoConfigureMockMvc
class LocalConfigurationEndpointsTest {
    static Path file;
    @DynamicPropertySource static void configuration(DynamicPropertyRegistry properties) throws Exception {
        ObjectMapper json=new ObjectMapper();
        ObjectNode state=(ObjectNode)json.readTree(Path.of("local-config/application-state.json").toFile());
        // No real accounts, hashes, sessions or SMTP secrets in the test fixture.
        for (String table: List.of("users","user_roles","password_history")) state.putArray(table);
        for (JsonNode setting: state.path("app_settings")) if (setting.path("sensible").asBoolean()) ((ObjectNode)setting).put("valeur", "");
        file=Files.createTempDirectory("monitoring-configuration-tests-").resolve("state.json");
        Files.writeString(file,json.writeValueAsString(state));
        properties.add("app.local-configuration.file", () -> file.toString());
        properties.add("spring.datasource.url", () -> "jdbc:h2:mem:configtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }
    @Autowired MockMvc mvc;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired MailConfigService mail;
    @Autowired LocalApplicationState state;
    private org.springframework.test.web.servlet.request.RequestPostProcessor admin;

    @BeforeEach void authenticate() {
        User account=users.findByLogin("local-validation").orElseGet(() -> {
            User u=new User(); u.setLogin("local-validation"); u.setEmail("local-validation@example.invalid"); u.setFirstName("Local"); u.setLastName("Validation"); u.setPasswordHash(passwords.encode("Local-check-2026"));
            u.setScope(UserScope.ADMIN); u.setRoles(new HashSet<>(Set.of(roles.findByCode("ROLE_ADMIN").orElseThrow())));
            return users.saveAndFlush(u);
        });
        admin=authentication(new UsernamePasswordAuthenticationToken(account,null,account.getAuthorities()));
    }

    @Test void normalLoginAndTokenRefreshWorkWithoutPostgres() throws Exception {
        String login=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"login\":\"local-validation\",\"password\":\"Local-check-2026\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.permissions").isNotEmpty()).andReturn().getResponse().getContentAsString();
        JsonNode auth=new ObjectMapper().readTree(login).path("data");
        mvc.perform(get("/api/users/me").header("Authorization","Bearer "+auth.path("accessToken").asText()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.login").value("local-validation"));
        String request=new ObjectMapper().writeValueAsString(Map.of("refreshToken",auth.path("refreshToken").asText()));
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(request)).andExpect(status().isOk());
        assertFalse(new ObjectMapper().readTree(file.toFile()).has("refresh_tokens"));
    }

    @Test void configurationRemainsAvailableWithoutPostgres() throws Exception {
        mvc.perform(get("/api/system/data-status").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.connected").value(false)).andExpect(jsonPath("$.configurationAvailable").value(true));
        mvc.perform(get("/api/roles").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
        mvc.perform(get("/api/roles/permissions").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isNotEmpty());
        mvc.perform(get("/api/monitoring/rules").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(43)).andExpect(jsonPath("$[0].alertCount").value(0));
        for (String group: List.of("MAIL","ALERTES","SECURITE","GENERAL")) mvc.perform(get("/api/settings/"+group).with(admin)).andExpect(status().isOk());
        assertInstanceOf(org.springframework.mail.javamail.JavaMailSenderImpl.class,mail.buildSender());
    }
    @Test void allBusinessListsAreEmptyAndCountersZero() throws Exception {
        for (String endpoint: List.of("atms","institutions","monitoring/alerts","fraud/reports/daily","transactions/rates","transactions/errors","transactions/operations","fleet/accounts","pos","notifications","extraction/jobs"))
            mvc.perform(get("/api/"+endpoint).with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/dashboard/summary").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalAtms").value(0)).andExpect(jsonPath("$.data.totalFrauds").value(0));
        mvc.perform(get("/api/dashboard/history").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.fraudHistory").isEmpty());
        mvc.perform(get("/api/fraud/alerts").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.alerts").isEmpty());
        mvc.perform(get("/api/institutions/all").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }
    @Test void noBusinessWritesAndNoBypassOfPermissions() throws Exception {
        mvc.perform(post("/api/institutions").with(admin).contentType("application/json").content("{}")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("BUSINESS_DATABASE_DISCONNECTED"));
        User denied=new User();denied.setId(1000L);denied.setLogin("denied");denied.setScope(UserScope.INSTITUTION);
        mvc.perform(get("/api/atms").with(authentication(new UsernamePasswordAuthenticationToken(denied,null,List.of())))).andExpect(status().isForbidden());
    }
    @Test void rolePermissionUpdatesAreStoredInFileWithoutBusinessData() throws Exception {
        Long operator=roles.findByCode("ROLE_OPERATOR").orElseThrow().getId();
        mvc.perform(put("/api/roles/"+operator+"/permissions").with(admin).contentType("application/json").content("{\"permissions\":[\"ATM_READ\"]}")).andExpect(status().isOk());
        JsonNode snapshot=new ObjectMapper().readTree(file.toFile());
        assertFalse(snapshot.has("institutions"));assertFalse(snapshot.has("atms"));assertFalse(snapshot.has("monitoring_alerts"));assertFalse(snapshot.has("refresh_tokens"));
        assertEquals(1, snapshot.path("role_permissions").findValues("role_id").stream().filter(n -> n.asLong()==operator).count());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM atms",Integer.class));
    }
}
