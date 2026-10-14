package org.intelligentsia.monitoring.domain.configuration.institution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "country_codes")
@Getter @Setter @NoArgsConstructor
public class CountryCode {
    @Id
    @Column(length = 20)
    private String code;
}
