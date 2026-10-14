package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * One row per fraud alert. rules field is a CSV of rule IDs.
 * status='1' means suspect; any other value means confirmed fraud.
 */
@Entity
@Table(name = "CmsFraud")
@Getter
public class CmsFraud {

    @Id
    private Long id;

    private String acquirer;

    private String issuer;

    private String bin;

    /** '1' = suspect transaction, else = confirmed fraud */
    private Character status;

    /** CSV of rule IDs that triggered this alert (e.g. "12,45,67") */
    private String rules;

    private String description;

    private String Risk_level;

    @Column(name = "risk_auth_code")
    private String riskAuthCode;

    @Column(name = "retrieval_reference")
    private String retrievalReference;

    public boolean isSuspect() {
        return status != null && status == '1';
    }

    public boolean isBelongToInstitution(String institutionCode) {
        return institutionCode != null
                && (institutionCode.equals(acquirer) || institutionCode.equals(issuer));
    }
}
