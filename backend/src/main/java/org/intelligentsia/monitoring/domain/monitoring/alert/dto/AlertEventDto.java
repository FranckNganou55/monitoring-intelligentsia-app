package org.intelligentsia.monitoring.domain.monitoring.alert.dto;

import lombok.Builder;
import lombok.Getter;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.AlertEvent;

import java.time.LocalDateTime;

@Getter
@Builder
public class AlertEventDto {

    private Long id;

    private Long alertId;

    private AlertEvent.EventType eventType;

    private Long actorUserId;

    private String oldStatus;

    private String newStatus;

    private String comment;

    private String details;

    private LocalDateTime createdAt;
}