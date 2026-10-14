package org.intelligentsia.monitoring.domain.configuration.settings.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_settings")
@Getter @Setter @NoArgsConstructor
public class AppSetting {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String cle;

    @Column(columnDefinition = "TEXT")
    private String valeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettingGroupe groupe;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "modifiable_par_admin", nullable = false)
    private boolean modifiableParAdmin = true;

    @Column(nullable = false)
    private boolean sensible = false;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
