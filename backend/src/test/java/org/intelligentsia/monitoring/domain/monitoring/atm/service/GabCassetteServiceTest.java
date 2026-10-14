package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import org.intelligentsia.monitoring.domain.monitoring.atm.dto.*;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.Atm;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmRepository;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.GabCassetteFixtureRepository;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.GabCassetteRepository;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.GabCassetteRepository.CassetteRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GabCassetteServiceTest {
    private final GabCassetteRepository source = mock(GabCassetteRepository.class);
    private final AtmRepository atms = mock(AtmRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<GabCassetteRepository> provider = mock(ObjectProvider.class);
    private final GabCassetteFixtureRepository fixtures = mock(GabCassetteFixtureRepository.class);
    private final GabCassetteService service = new GabCassetteService(provider, atms, fixtures);

    private Atm atm(Long code, Long institutionId) {
        Institution institution = new Institution(); institution.setId(institutionId);
        Atm atm = new Atm(); atm.setInstitution(institution); atm.setSvfePid(code); return atm;
    }
    @Test void groupsAndSortsWithoutReplacingNullsOrZero() {
        GabCassetteDto missing = new GabCassetteDto(2, "28", new BigDecimal("100"), null, null, null, null, LocalDateTime.of(2026, 4, 11, 11, 58, 1));
        GabCassetteDto zero = new GabCassetteDto(1, "28", new BigDecimal("100"), 0L, 0L, 0L, BigDecimal.ZERO, null);
        var result = GabCassetteService.group(List.of(new CassetteRow(2L, "00002", "Deux", missing), new CassetteRow(1L, "00001", "Un", missing), new CassetteRow(1L, "00001", "Un", zero)));
        assertEquals(2, result.size()); assertEquals("00001", result.get(0).numeroGab());
        assertEquals(1, result.get(0).cassettes().get(0).cassetteNum());
        assertEquals(0L, result.get(0).cassettes().get(0).billetsRestants());
        assertNull(result.get(0).cassettes().get(1).soldeCassette());
        assertNull(result.get(0).cassettes().get(1).billetsRestants());
    }
    @Test void detailUsesOracleCodeRatherThanLocalId() {
        when(provider.getIfAvailable()).thenReturn(source);
        when(atms.findById(3L)).thenReturn(Optional.of(atm(1067780L, 1L)));
        when(source.findLatest(List.of(1067780L))).thenReturn(List.of());
        assertTrue(service.findLatest(3L, 1L).isEmpty()); verify(source).findLatest(List.of(1067780L));
    }
    @Test void anotherInstitutionCannotReadDetail() {
        when(atms.findById(3L)).thenReturn(Optional.of(atm(1067780L, 2L)));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.findLatest(3L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus()); verifyNoInteractions(source);
    }
    @Test void unlinkedGabDoesNotFallBackToAllOracleRows() {
        when(atms.findById(3L)).thenReturn(Optional.of(atm(null, 1L)));
        assertEquals("ATM_NOT_LINKED", assertThrows(BusinessException.class, () -> service.findLatest(3L, null)).getCode());
        verifyNoInteractions(source);
    }
    @Test void emptyInstitutionNeverQueriesAllCassettes() {
        when(provider.getIfAvailable()).thenReturn(source);
        when(atms.findAllByInstitution(1L)).thenReturn(List.of());
        assertTrue(service.findLatest(null, 1L).isEmpty()); verifyNoInteractions(source);
    }
    @Test void disabledOracleReturnsExplicitUnavailable() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(BusinessException.class, () -> service.findLatest(null, null)).getStatus());
    }
    @Test void oracleFailureReturnsAnErrorRatherThanAnEmptySuccess() {
        when(provider.getIfAvailable()).thenReturn(source);
        when(source.findLatest(null)).thenThrow(new DataAccessResourceFailureException("offline"));
        assertEquals("ORACLE_CASSETTES_UNAVAILABLE", assertThrows(BusinessException.class, () -> service.findLatest(null, null)).getCode());
    }

    @Test void explicitTestGabReadsExcelFixtureWithoutOracle() {
        when(atms.findById(5L)).thenReturn(Optional.of(atm(null, 1L)));
        var fixture = new GabCassettesDto(1067780L, "108401001", "GAB N1 HAI CINEMA", List.of(), "TEST_EXCEL");
        when(fixtures.findByAtmId(5L)).thenReturn(Optional.of(fixture));
        assertEquals("TEST_EXCEL", service.findLatest(5L, 1L).get(0).source());
        verifyNoInteractions(source);
    }
    @Test void anotherInstitutionCannotReadExcelFixture() {
        when(atms.findById(5L)).thenReturn(Optional.of(atm(null, 2L)));
        assertThrows(BusinessException.class, () -> service.findLatest(5L, 1L));
        verifyNoInteractions(fixtures);
    }
}
