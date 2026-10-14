package org.intelligentsia.monitoring.domain.audit.repository;
import org.intelligentsia.monitoring.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
        SELECT a FROM AuditLog a WHERE
        (:userId IS NULL OR a.userId = :userId)
        AND (:entityType IS NULL OR a.entityType = :entityType)
        AND (:action IS NULL OR a.action = :action)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> findWithFilters(Long userId, String entityType, String action, Pageable pageable);
}
