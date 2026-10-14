package org.intelligentsia.monitoring.infrastructure.scheduler.tasks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.monitoring.atm.service.AtmInterfaceMonitorService;
import org.intelligentsia.monitoring.infrastructure.svfe.SvfeGateway;
import org.springframework.stereotype.Component;
@Slf4j @Component @RequiredArgsConstructor
public class AtmQueryTask implements Runnable {
    private final SvfeGateway svfe;
    private final AtmInterfaceMonitorService interfaceMonitor;
    @Override public void run() {
        log.info("Début requête ATM depuis SVFE");
        interfaceMonitor.collectAndEvaluate();
        svfe.syncAtms();
        log.info("Fin requête ATM");
    }
}
