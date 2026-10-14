package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * Fleet account balance from SVFE (solde flotte).
 * currency is the ISO numeric code as a String (e.g. "952").
 * Previous balance is derived by querying the last saved FleetAccount snapshot.
 */
@Entity
@Table(name = "CmsAccount")
@Getter
public class CmsAccount {

    @Id
    private Long id;

    private String institution;

    /** ISO numeric currency code (e.g. "952" for XOF) */
    private String currency;

    private String account;

    private Double balance;
}
