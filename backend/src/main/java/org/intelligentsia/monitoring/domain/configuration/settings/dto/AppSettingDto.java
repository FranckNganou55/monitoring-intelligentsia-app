package org.intelligentsia.monitoring.domain.configuration.settings.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class AppSettingDto {
    private String  cle;
    private String  valeur;   // null when sensible=true (never returned to client)
    private String  type;
    private String  groupe;
    private String  label;
    private String  description;
    private boolean modifiableParAdmin;
    private boolean sensible;
    private boolean sensibleConfigured;
    private Instant updatedAt;
    private String  updatedBy;
}
