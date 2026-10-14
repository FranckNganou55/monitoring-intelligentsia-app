package org.intelligentsia.monitoring.domain.configuration.institution.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InstitutionAlertRoutingRequest {
    @Pattern(regexp = "[0-9]{1,20}")
    private String countryNumericCode;
    @Email @Size(max = 255) private String alertEmailTypique;
    @Email @Size(max = 255) private String alertEmailMoinsCritique;
    @Email @Size(max = 255) private String alertEmailMoyen;
    @Email @Size(max = 255) private String alertEmailCritique;
}
