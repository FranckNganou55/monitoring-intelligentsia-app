package org.intelligentsia.monitoring.infrastructure.scheduler.tasks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.infrastructure.svfe.SvfeGateway;
import org.springframework.stereotype.Component;
@Slf4j @Component @RequiredArgsConstructor
public class HostQueryTask implements Runnable {
    private final SvfeGateway svfe;
    @Override public void run() {
        log.info("Début requête hôtes depuis SVFE");
        svfe.syncHosts();
        log.info("Fin requête hôtes");
    }
}
