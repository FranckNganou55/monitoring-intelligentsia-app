package org.intelligentsia.monitoring.domain.audit.service;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.audit.dto.AuditLogDto;
import org.intelligentsia.monitoring.domain.audit.entity.AuditLog;
import org.intelligentsia.monitoring.domain.audit.repository.AuditLogRepository;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
@Service @RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;
    @Transactional
    public void log(Long userId, String userLogin, String action, String entityType, String entityId,
                    Map<String,Object> oldValue, Map<String,Object> newValue, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setUserId(userId); log.setUserLogin(userLogin); log.setAction(action);
        log.setEntityType(entityType); log.setEntityId(entityId);
        log.setOldValue(oldValue); log.setNewValue(newValue);
        log.setIpAddress(ipAddress); log.setUserAgent(userAgent);
        repo.save(log);
    }
    @Transactional(readOnly=true)
    public PageResponse<AuditLogDto> findAll(Long userId, String entityType, String action, int page, int size) {
        return PageResponse.of(repo.findWithFilters(userId, entityType, action,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toDto));
    }
    private AuditLogDto toDto(AuditLog a) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(a.getId()); dto.setUserId(a.getUserId()); dto.setUserLogin(a.getUserLogin());
        dto.setAction(a.getAction()); dto.setEntityType(a.getEntityType()); dto.setEntityId(a.getEntityId());
        dto.setOldValue(a.getOldValue()); dto.setNewValue(a.getNewValue());
        dto.setIpAddress(a.getIpAddress()); dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
