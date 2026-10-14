package org.intelligentsia.monitoring.infrastructure.startup;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Repair missing role catalogue access on already migrated legacy databases. */
@org.springframework.context.annotation.Profile("!local-config")
@Component
@RequiredArgsConstructor
public class RolePermissionInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional("transactionManager")
    public void run(ApplicationArguments args) {
        jdbc.update("""
                INSERT INTO permissions (code, name, resource, action)
                VALUES ('ROLE_READ', 'Consulter les rôles', 'ROLE', 'READ')
                ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                WHERE r.code = 'ROLE_ADMIN' AND p.code = 'ROLE_READ'
                ON CONFLICT (role_id, permission_id) DO NOTHING
                """);
    }
}
