package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

/**
 * POS terminals pending telecollect in SVFE (those not yet telecollected today).
 * A terminal in this table is "NON OK"; terminals absent from this table are "OK".
 */
@Entity
@Table(name = "CmsTeleCollect")
@Getter
public class CmsTeleCollect {

    @Id
    private String terminalID;

    private String institution;

    private LocalDate collectDate;
}
