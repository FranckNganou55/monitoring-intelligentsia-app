package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "CmsHost")
@Getter
public class CmsHost {

    @Id
    private String pid;

    private String description;

    /** Operational status: typically 'Y' (connected) or 'N' (disconnected) */
    private Character opStat;
}
