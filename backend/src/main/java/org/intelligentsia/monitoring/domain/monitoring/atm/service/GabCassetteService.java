package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import org.intelligentsia.monitoring.domain.monitoring.atm.dto.GabCassettesDto;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.Atm;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmRepository;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.GabCassetteFixtureRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.GabCassetteRepository;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.GabCassetteRepository.CassetteRow;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GabCassetteService {
    private final ObjectProvider<GabCassetteRepository> source;
    private final AtmRepository atms;
    private final GabCassetteFixtureRepository fixtures;

    public GabCassetteService(ObjectProvider<GabCassetteRepository> source, AtmRepository atms, GabCassetteFixtureRepository fixtures) {
        this.source = source;
        this.atms = atms;
        this.fixtures = fixtures;
    }

    public List<GabCassettesDto> findLatest(Long atmId, Long institutionId) {
        List<Long> codes = null;
        if (atmId != null) {
            Atm atm = atms.findById(atmId).filter(a -> institutionId == null
                    || institutionId.equals(a.getInstitution().getId())).orElseThrow(() ->
                    new BusinessException("ATM_NOT_FOUND", "GAB introuvable.", HttpStatus.NOT_FOUND));
            var fixture = fixtures.findByAtmId(atmId);
            if (fixture.isPresent()) return List.of(fixture.get());
            if (atm.getSvfePid() == null) throw new BusinessException("ATM_NOT_LINKED",
                    "Ce GAB n’est pas encore rattaché à son code Oracle.", HttpStatus.CONFLICT);
            codes = List.of(atm.getSvfePid());
        } else if (institutionId != null) {
            codes = atms.findAllByInstitution(institutionId).stream().map(Atm::getSvfePid)
                    .filter(java.util.Objects::nonNull).distinct().sorted().toList();
        }
        GabCassetteRepository repository = source.getIfAvailable();
        if (repository == null) throw unavailable("La connexion Oracle des cassettes n’est pas activée.");
        List<CassetteRow> rows = new ArrayList<>();
        try {
            if (codes == null) rows.addAll(repository.findLatest(null));
            // Oracle IN lists are limited to 1000 expressions on existing supported installations.
            else for (int start = 0; start < codes.size(); start += 900)
                rows.addAll(repository.findLatest(codes.subList(start, Math.min(start + 900, codes.size()))));
        } catch (DataAccessException ex) {
            throw unavailable("Impossible de lire les cassettes dans Oracle. Vérifiez la connexion et les droits de lecture.");
        }
        return group(rows);
    }

    static List<GabCassettesDto> group(List<CassetteRow> rows) {
        Map<Long, List<CassetteRow>> grouped = new LinkedHashMap<>();
        rows.stream().sorted(Comparator.comparing(CassetteRow::codeGab)
                .thenComparing(row -> row.cassette().cassetteNum(), Comparator.nullsLast(Integer::compareTo)))
                .forEach(row -> grouped.computeIfAbsent(row.codeGab(), key -> new ArrayList<>()).add(row));
        return grouped.values().stream().map(group -> {
            CassetteRow first = group.get(0);
            return new GabCassettesDto(first.codeGab(), first.numeroGab(), first.nomGab(),
                    group.stream().map(CassetteRow::cassette).toList());
        }).toList();
    }

    private BusinessException unavailable(String message) {
        return new BusinessException("ORACLE_CASSETTES_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
