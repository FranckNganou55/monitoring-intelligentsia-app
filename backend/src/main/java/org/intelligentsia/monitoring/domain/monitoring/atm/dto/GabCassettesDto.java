package org.intelligentsia.monitoring.domain.monitoring.atm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GabCassettesDto(Long codeGab, String numeroGab, String nomGab,
        List<GabCassetteDto> cassettes, String source) {
    public GabCassettesDto(Long codeGab, String numeroGab, String nomGab, List<GabCassetteDto> cassettes) {
        this(codeGab, numeroGab, nomGab, cassettes, "ORACLE");
    }
}
