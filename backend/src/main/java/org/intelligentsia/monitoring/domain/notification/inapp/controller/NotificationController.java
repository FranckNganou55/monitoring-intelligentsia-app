package org.intelligentsia.monitoring.domain.notification.inapp.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.notification.inapp.dto.NotificationDto;
import org.intelligentsia.monitoring.domain.notification.inapp.service.*;
import org.intelligentsia.monitoring.shared.dto.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
@Tag(name="Notifications") @RestController @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifService;
    private final SseEmitterService sseService;

    @GetMapping(value="/api/sse/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal User user) {
        return sseService.subscribe(user.getId());
    }

    @GetMapping("/api/notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) Long institutionId) {
        return ResponseEntity.ok(ApiResponse.ok(notifService.findByUser(user.getId(), page, size,
                effectiveInstitutionId(user, institutionId))));
    }

    @GetMapping("/api/notifications/open-alert-ids")
    public ResponseEntity<ApiResponse<java.util.List<Long>>> openAlertIds(
            @AuthenticationPrincipal User user,
            @RequestParam(required=false) Long institutionId) {
        return ResponseEntity.ok(ApiResponse.ok(notifService.findOpenAlertIds(user.getId(),
                effectiveInstitutionId(user, institutionId))));
    }

    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String,Long>>> unreadCount(
            @AuthenticationPrincipal User user,
            @RequestParam(required=false) Long institutionId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", notifService.countUnread(user.getId(),
                effectiveInstitutionId(user, institutionId)))));
    }

    @PostMapping("/api/notifications/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal User user,
            @RequestParam(required=false) Long institutionId) {
        notifService.markAllRead(user.getId(), effectiveInstitutionId(user, institutionId));
        return ResponseEntity.ok(ApiResponse.ok("Toutes les notifications marquées comme lues."));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestParam(required=false) Long institutionId) {
        notifService.markRead(id, user.getId(), effectiveInstitutionId(user, institutionId));
        return ResponseEntity.ok(ApiResponse.ok("Notification marquée comme lue."));
    }

    private Long effectiveInstitutionId(User user, Long selectedInstitutionId) {
        return user.getScope() == UserScope.INSTITUTION && user.hasInstitution()
                ? user.getInstitution().getId() : selectedInstitutionId;
    }
}
