package org.intelligentsia.monitoring.domain.monitoring.atm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GabCassetteDto(Integer cassetteNum, String devise, BigDecimal valeurBillet,
        Long billetsRestants, Long billetsRejetes, Long billetsDispenses,
        BigDecimal soldeCassette, LocalDateTime derniereMaj) {}
