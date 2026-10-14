package org.intelligentsia.monitoring.domain.audit.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
@Entity @Table(name="audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id") private Long userId;
    @Column(name="user_login",length=100) private String userLogin;
    @Column(nullable=false,length=50) private String action;
    @Column(name="entity_type",length=100) private String entityType;
    @Column(name="entity_id",length=100) private String entityId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="old_value",columnDefinition="jsonb") private Map<String,Object> oldValue;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="new_value",columnDefinition="jsonb") private Map<String,Object> newValue;
    @Column(name="ip_address",length=50) private String ipAddress;
    @Column(name="user_agent",length=500) private String userAgent;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
}
