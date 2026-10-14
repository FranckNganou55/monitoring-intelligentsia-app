package org.intelligentsia.monitoring.domain.auth.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.intelligentsia.monitoring.infrastructure.scheduler.MonitoringScheduler;
import org.intelligentsia.monitoring.domain.extraction.service.ExtractionSchedulerService;
import org.intelligentsia.monitoring.domain.auth.entity.*;
import org.intelligentsia.monitoring.domain.auth.repository.*;
import org.intelligentsia.monitoring.infrastructure.security.JwtTokenProvider;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@EnabledIfSystemProperty(named="monitoring.dbTests", matches="true")
@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://localhost:5432/monitoring_test", "app.svfe.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class RoleEndpointsTest {
    @Autowired MockMvc mvc;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository institutions;
    @Autowired RoleService service;
    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokens;
    @Autowired jakarta.persistence.EntityManager em;
    @MockitoBean MonitoringScheduler scheduler;
    @MockitoBean ExtractionSchedulerService extractionScheduler;

    @Test void onlyAdminCanMutate() throws Exception {
        mvc.perform(post("/api/roles").with(user("operator").roles("OPERATOR")).contentType("application/json").content("{\"code\":\"ROLE_NEW\",\"name\":\"New\",\"permissions\":[]}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/roles/permissions").with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/roles").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"code\":\"invalid\",\"name\":\"New\",\"permissions\":[]}")).andExpect(status().isUnprocessableEntity());
        Long id=roles.findByCode("ROLE_ADMIN").orElseThrow().getId();
        mvc.perform(put("/api/roles/"+id+"/permissions").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"permissions\":[]}")).andExpect(status().isForbidden());
    }

    @Test void sameJwtLosesAndRegainsPermissionWithoutLogin() throws Exception {
        Role custom=service.create("ROLE_LIVE_TEST", "Live Test", null, Set.of("ROLE_READ"));
        User account=users.findAll().stream().filter(u -> u.isEnabled() && u.isAccountNonExpired() && u.isCredentialsNonExpired()).findFirst().orElseThrow();
        var request=new org.intelligentsia.monitoring.domain.auth.dto.UserUpdateRequest();
        request.setFirstName(account.getFirstName()); request.setLastName(account.getLastName()); request.setEmail(account.getEmail());
        request.setRoleIds(Set.of(custom.getId()));
        request.setInstitutionId(institutions.findAll().get(0).getId());
        userService.update(account.getId(), request); em.flush(); String login=account.getLogin(); Long accountId=account.getId(); em.clear();
        assertEquals(Set.of("ROLE_LIVE_TEST"), users.findById(accountId).orElseThrow().getRoles().stream().map(Role::getCode).collect(java.util.stream.Collectors.toSet()));
        String token=tokens.generateAccessToken(login);
        mvc.perform(get("/api/roles").header("Authorization", "Bearer "+token)).andExpect(status().isOk());
        service.updatePermissions(custom.getId(), Set.of()); em.flush(); em.clear();
        mvc.perform(get("/api/roles").header("Authorization", "Bearer "+token)).andExpect(status().isForbidden());
        service.updatePermissions(custom.getId(), Set.of("ROLE_READ")); em.flush(); em.clear();
        mvc.perform(get("/api/roles").header("Authorization", "Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.permissions[0]").value("ROLE_READ"));
    }
}
