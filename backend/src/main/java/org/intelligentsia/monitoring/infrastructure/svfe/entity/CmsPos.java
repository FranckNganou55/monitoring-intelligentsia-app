package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * POS terminal configuration in SVFE. terminalID is the PK (capital D matches Oracle column name).
 */
@Entity
@Table(name = "CmsPos")
@Getter
public class CmsPos {

    @Id
    private String terminalID;

    private String merchant;

    private String ville;

    private String quartier;

    private String institution;
}
