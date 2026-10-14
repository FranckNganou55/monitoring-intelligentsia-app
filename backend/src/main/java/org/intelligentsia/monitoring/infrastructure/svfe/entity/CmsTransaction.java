package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * One row per transaction response code + network + acquirer/issuer combination
 * within the query time window. responseCode "00" = success.
 */
@Entity
@Table(name = "CmsTransaction")
@Getter
public class CmsTransaction {

    @Id
    private Long id;

    private String network;

    private String responseCode;

    private String acquirer;

    private String issuer;

    private String responseDesc;

    /** Transaction count for this row */
    private Integer nbre;

    public boolean isSuccess() {
        return "00".equals(responseCode);
    }
}
