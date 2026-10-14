package org.intelligentsia.monitoring.domain.configuration.institution.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class InstitutionDto {
    private Long id;
    private String code;
    private String name;
    private String svfeId;
    private String hostPid;
    private String mailRecharge;
    private String countryNumericCode;
    private String alertEmailTypique;
    private String alertEmailMoinsCritique;
    private String alertEmailMoyen;
    private String alertEmailCritique;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CurrencyThresholdDto> currencies;

    @Data
    public static class CurrencyThresholdDto {
        private Long currencyId;
        private String isoAlpha;
        private String label;
        private java.math.BigDecimal atmThreshold;
        private java.math.BigDecimal cardPpLimit;
    }
}
