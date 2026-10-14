package org.intelligentsia.monitoring.domain.configuration.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AtmInterfaceSettingsRequest(@Min(1) @Max(20) int samples,
                                          @Min(60000) @Max(900000) int intervalMs) {}
