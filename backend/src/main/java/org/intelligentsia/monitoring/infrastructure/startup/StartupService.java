package org.intelligentsia.monitoring.infrastructure.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupService implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // A server restart must not invalidate browser sessions. Refresh tokens are
        // revoked on logout, password reset, or a new login for the same user.
        log.info("[STARTUP] Démarrage serveur — sessions existantes conservées.");
    }
}
