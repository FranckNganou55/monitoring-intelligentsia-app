package org.intelligentsia.monitoring.domain.monitoring.atm.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.GabCassettesDto;
import org.intelligentsia.monitoring.domain.monitoring.atm.service.GabCassetteService;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Cassettes GAB")
@RestController
@RequestMapping("/api/gabs/cassettes")
public class GabCassetteController {
    private final GabCassetteService service;
    public GabCassetteController(GabCassetteService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('ATM_READ')")
    public ApiResponse<List<GabCassettesDto>> list(@RequestParam(required = false) Long atmId,
            @RequestParam(required = false) Long institutionId, @AuthenticationPrincipal User user) {
        Long effectiveId = institutionId;
        if (user.getScope() == UserScope.INSTITUTION) {
            if (!user.hasInstitution()) throw new BusinessException("MISSING_INSTITUTION",
                    "Votre compte n’est rattaché à aucune institution.", HttpStatus.FORBIDDEN);
            effectiveId = user.getInstitution().getId();
        }
        return ApiResponse.ok(service.findLatest(atmId, effectiveId));
    }
}
