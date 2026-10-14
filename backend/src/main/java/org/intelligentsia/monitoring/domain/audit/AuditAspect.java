package org.intelligentsia.monitoring.domain.audit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.intelligentsia.monitoring.domain.audit.service.AuditService;
import org.intelligentsia.monitoring.shared.annotation.Auditable;
import org.intelligentsia.monitoring.shared.util.SecurityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.lang.reflect.Method;
@Slf4j @Aspect @Component @RequiredArgsConstructor
public class AuditAspect {
    private final AuditService auditService;
    @Around("@annotation(org.intelligentsia.monitoring.shared.annotation.Auditable)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);
        Object result = pjp.proceed();
        try {
            Long userId = SecurityUtils.getCurrentUserId().orElse(null);
            String login = SecurityUtils.getCurrentUserLogin().orElse("anonymous");
            String ip = null; String ua = null;
            try {
                HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
                ip = req.getRemoteAddr(); ua = req.getHeader("User-Agent");
            } catch (Exception ignored) {}
            auditService.log(userId, login, auditable.action(), auditable.entityType(), null, null, null, ip, ua);
        } catch (Exception e) {
            log.error("Erreur lors de l'audit: {}", e.getMessage());
        }
        return result;
    }
}
