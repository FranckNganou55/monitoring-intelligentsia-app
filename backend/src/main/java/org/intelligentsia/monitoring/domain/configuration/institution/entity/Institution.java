package org.intelligentsia.monitoring.domain.configuration.institution.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "institutions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class Institution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "svfe_id", length = 50)
    private String svfeId;
    @Column(name = "host_pid", length = 50)
    private String hostPid;
    @Column(name = "mail_recharge", length = 255)
    private String mailRecharge;
    @Column(name = "country_numeric_code", length = 20)
    private String countryNumericCode;
    @Column(name = "alert_email_typique", length = 255)
    private String alertEmailTypique;
    @Column(name = "alert_email_moins_critique", length = 255)
    private String alertEmailMoinsCritique;
    @Column(name = "alert_email_moyen", length = 255)
    private String alertEmailMoyen;
    @Column(name = "alert_email_critique", length = 255)
    private String alertEmailCritique;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @CreatedBy @Column(name = "created_by")
    private Long createdBy;
    @LastModifiedBy @Column(name = "updated_by")
    private Long updatedBy;
    @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY)
    private List<InstitutionCurrency> currencies = new ArrayList<>();
}
