package org.intelligentsia.monitoring.domain.notification.inapp.service;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.notification.inapp.dto.NotificationDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.compute(userId, (id, sessions) -> {
            if (sessions == null) sessions = ConcurrentHashMap.newKeySet();
            sessions.add(emitter);
            return sessions;
        });
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        sendEvent(userId, emitter, "connected", "SSE connecté");
        return emitter;
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (id, sessions) -> {
            sessions.remove(emitter);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    private void sendEvent(Long userId, SseEmitter emitter, String event, Object data) {
        try { emitter.send(SseEmitter.event().name(event).data(data)); }
        catch (IOException | IllegalStateException e) {
            remove(userId, emitter);
            log.debug("Session SSE fermée pour l'utilisateur {}", userId);
        }
    }

    public void send(Long userId, NotificationDto notification) {
        Set<SseEmitter> sessions = emitters.get(userId);
        if (sessions != null) sessions.forEach(emitter -> sendEvent(userId, emitter, "notification", notification));
    }

    public void permissionsChanged() {
        emitters.forEach((id, sessions) -> sessions.forEach(emitter -> sendEvent(id, emitter, "permissions-changed", "refresh")));
    }

    public void broadcast(NotificationDto notification) {
        emitters.forEach((id, sessions) -> send(id, notification));
    }
}
