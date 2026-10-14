package org.intelligentsia.monitoring.domain.audit.dto;
import lombok.Data;
import java.time.Instant;
import java.util.Map;
@Data
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String userLogin;
    private String action;
    private String entityType;
    private String entityId;
    private Map<String,Object> oldValue;
    private Map<String,Object> newValue;
    private String ipAddress;
    private Instant createdAt;
}
