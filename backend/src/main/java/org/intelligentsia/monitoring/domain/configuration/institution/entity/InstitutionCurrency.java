package org.intelligentsia.monitoring.domain.configuration.institution.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "institution_currencies") @Getter @Setter @NoArgsConstructor
public class InstitutionCurrency {
    @EmbeddedId private InstitutionCurrencyId id = new InstitutionCurrencyId();
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("institutionId") @JoinColumn(name = "institution_id")
    private Institution institution;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("currencyId") @JoinColumn(name = "currency_id")
    private Currency currency;
    @Column(name = "atm_threshold", precision = 20, scale = 2) private BigDecimal atmThreshold;
    @Column(name = "card_pp_limit", precision = 20, scale = 2) private BigDecimal cardPpLimit;
}
