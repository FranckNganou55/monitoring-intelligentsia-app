package org.intelligentsia.monitoring.domain.notification.inapp.repository;
import org.intelligentsia.monitoring.domain.notification.inapp.entity.Notification;
import org.intelligentsia.monitoring.domain.monitoring.alert.entity.MonitoringAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
          AND (:institutionId IS NULL OR n.alert.institutionId = :institutionId OR n.alert.atm.institution.id = :institutionId)
          AND (n.read = false OR n.readAt >= :visibleAfter)
        ORDER BY n.createdAt DESC
        """)
    Page<Notification> findVisibleByUserAndInstitution(
            @Param("userId") Long userId,
            @Param("institutionId") Long institutionId,
            @Param("visibleAfter") Instant visibleAfter,
            Pageable pageable);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.user.id = :userId AND n.read = false
          AND (:institutionId IS NULL OR n.alert.institutionId = :institutionId OR n.alert.atm.institution.id = :institutionId)
        """)
    long countUnreadByUserAndInstitution(@Param("userId") Long userId, @Param("institutionId") Long institutionId);

    @Query("""
        SELECT DISTINCT n.alert.id FROM Notification n
        WHERE n.user.id = :userId AND n.type = 'ALERT' AND n.alert IS NOT NULL
          AND n.alert.status = :status
          AND (:institutionId IS NULL OR n.alert.institutionId = :institutionId OR n.alert.atm.institution.id = :institutionId)
        """)
    List<Long> findAlertIdsByUserAndStatus(@Param("userId") Long userId,
                                           @Param("status") MonitoringAlert.AlertStatus status,
                                           @Param("institutionId") Long institutionId);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.read = true, n.readAt = COALESCE(n.readAt, :readAt)
        WHERE n.user.id = :userId AND n.read = false
          AND (:institutionId IS NULL OR n.alert.institutionId = :institutionId OR n.alert.atm.institution.id = :institutionId)
        """)
    int markAllReadByUserAndInstitution(@Param("userId") Long userId,
                                       @Param("institutionId") Long institutionId,
                                       @Param("readAt") Instant readAt);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.read = true, n.readAt = COALESCE(n.readAt, :readAt)
        WHERE n.id = :notificationId AND n.user.id = :userId
          AND (:institutionId IS NULL OR n.alert.institutionId = :institutionId OR n.alert.atm.institution.id = :institutionId)
        """)
    int markReadByIdAndUser(@Param("notificationId") Long notificationId,
                            @Param("userId") Long userId,
                            @Param("institutionId") Long institutionId,
                            @Param("readAt") Instant readAt);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.read = true AND n.readAt IS NOT NULL AND n.readAt < :cutoff")
    int deleteReadBefore(@Param("cutoff") Instant cutoff);
}
