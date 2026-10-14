package org.intelligentsia.monitoring.domain.monitoring.fraud.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.monitoring.fraud.dto.FraudDetailDto;
import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsFraud;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.CmsFraudRepository;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SvfeFraudReadService {
    private final ObjectProvider<CmsFraudRepository> cmsFraudRepository;
    private final InstitutionRepository institutionRepository;

    @Value("${app.svfe.enabled:false}")
    private boolean svfeEnabled;

    public PageResponse<FraudDetailDto> find(FilterRequest filter, Long institutionId) {
        CmsFraudRepository repository = cmsFraudRepository.getIfAvailable();
        if (!svfeEnabled || repository == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SVFE est désactivé. Configurez SVFE_ENABLED, SVFE_DB_URL, SVFE_DB_USER et SVFE_DB_PASS.");
        }

        Map<String, Institution> institutionsBySvfeId = new HashMap<>();
        institutionRepository.findAll().stream()
                .filter(i -> i.getSvfeId() != null && !i.getSvfeId().isBlank())
                .forEach(i -> institutionsBySvfeId.putIfAbsent(normalize(i.getSvfeId()), i));

        Instant collectedAt = Instant.now();
        String search = filter.getSearch() == null ? "" : filter.getSearch().trim().toLowerCase(Locale.ROOT);
        List<FraudDetailDto> rows = new ArrayList<>();
        for (CmsFraud fraud : repository.findFraud()) {
            List<Institution> matchedInstitutions = institutionsBySvfeId.values().stream()
                    .distinct()
                    .filter(i -> fraud.isBelongToInstitution(i.getSvfeId()))
                    .filter(i -> institutionId == null || institutionId.equals(i.getId()))
                    .toList();

            // Administrators can still inspect source records before institutions are mapped.
            if (matchedInstitutions.isEmpty() && institutionId == null) {
                matchedInstitutions = Collections.singletonList(null);
            }
            if (matchedInstitutions.isEmpty()) continue;

            List<String> rules = fraud.getRules() == null || fraud.getRules().isBlank()
                    ? List.of("—")
                    : Arrays.stream(fraud.getRules().split(","))
                            .map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
            for (Institution institution : matchedInstitutions) {
                for (int i = 0; i < rules.size(); i++) {
                    String ruleCode = rules.get(i);
                    FraudDetailDto row = toDto(fraud, institution, ruleCode, collectedAt);
                    String searchable = String.join(" ",
                            Objects.toString(row.getBin(), ""),
                            Objects.toString(row.getSourceReference(), ""),
                            Objects.toString(row.getRuleCode(), ""),
                            Objects.toString(row.getRuleDesc(), ""),
                            Objects.toString(row.getDescription(), ""),
                            Objects.toString(row.getInstitutionName(), ""))
                            .toLowerCase(Locale.ROOT);
                    if (search.isEmpty() || searchable.contains(search)) rows.add(row);
                }
            }
        }

        int page = Math.max(0, filter.getPage());
        int size = Math.min(100, Math.max(1, filter.getSize()));
        return PageResponse.of(new PageImpl<>(rows, PageRequest.of(page, size), rows.size()));
    }

    private FraudDetailDto toDto(CmsFraud fraud, Institution institution, String ruleCode, Instant collectedAt) {
        FraudDetailDto dto = new FraudDetailDto();
        String reference = fraud.getRiskAuthCode() != null && !fraud.getRiskAuthCode().isBlank()
                ? "RA:" + fraud.getRiskAuthCode().trim()
                : fraud.getRetrievalReference() == null || fraud.getRetrievalReference().isBlank()
                    ? null : "RRN:" + fraud.getRetrievalReference().trim();
        dto.setId(Integer.toUnsignedLong(Objects.hash(
                reference, fraud.getBin(), ruleCode, institution == null ? null : institution.getId())));
        dto.setInstitutionId(institution == null ? null : institution.getId());
        dto.setInstitutionName(institution == null
                ? "SVFE " + Objects.toString(fraud.getAcquirer(), "—") + " / " + Objects.toString(fraud.getIssuer(), "—")
                : institution.getName());
        dto.setBin(fraud.getBin());
        dto.setRuleCode(ruleCode);
        dto.setRuleDesc(fraud.getDescription());
        dto.setSourceReference(reference);
        dto.setSourceRiskLevel(fraud.getRisk_level());
        dto.setRiskLevel(normalizeRisk(fraud.getRisk_level()));
        dto.setSuspectCount(fraud.isSuspect() ? 1 : 0);
        dto.setFraudCount(fraud.isSuspect() ? 0 : 1);
        dto.setDescription(fraud.getDescription());
        dto.setSnapshotTime(collectedAt);
        return dto;
    }

    private String normalizeRisk(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_")) {
            case "CRITIQUE", "CRITICAL", "CRIT" -> "CRITIQUE";
            case "MOYEN", "MEDIUM", "MODERATE" -> "MOYEN";
            case "TYPIQUE", "TYPICAL", "LOW", "FAIBLE" -> "TYPIQUE";
            case "MOINS_CRITIQUE", "LESS_CRITICAL", "LESSCRITICAL" -> "MOINS_CRITIQUE";
            default -> null;
        };
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
