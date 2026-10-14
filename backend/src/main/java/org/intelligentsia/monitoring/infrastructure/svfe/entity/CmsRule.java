package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * Fraud detection rules in SVFE. Used to resolve rule IDs from CmsFraud.rules (CSV) to descriptions.
 */
@Entity
@Table(name = "CmsRule")
@Getter
public class CmsRule {

    @Id
    private String ruleId;

    private String ruleDescription;

    private String ruleStatus;
}
