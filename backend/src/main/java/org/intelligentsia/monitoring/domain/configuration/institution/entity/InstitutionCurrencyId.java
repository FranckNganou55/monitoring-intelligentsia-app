package org.intelligentsia.monitoring.domain.configuration.institution.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable @Getter @Setter @NoArgsConstructor @EqualsAndHashCode
public class InstitutionCurrencyId implements Serializable {
    private Long institutionId;
    private Long currencyId;
}
