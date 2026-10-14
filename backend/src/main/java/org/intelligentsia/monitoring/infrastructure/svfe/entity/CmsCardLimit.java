package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * Cards that have exceeded their PP limit. The repository query already filters
 * by institution + currency + limitAmount, so all returned rows are over-limit.
 * balance here is the card's current usage amount.
 */
@Entity
@Table(name = "CmsCardLimit")
@Getter
public class CmsCardLimit {

    @Id
    private Long id;

    private String institution;

    private String cardNumber;

    private String embossName;

    /** ISO numeric currency code */
    private String currency;

    private String account;

    /** Current card usage amount (compared against the limit from InstitutionCurrency) */
    private Double balance;
}
