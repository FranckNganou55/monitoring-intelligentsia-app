# Cassettes Oracle et jeu Excel — fichiers complets

Guides : [API Oracle](cassettes-oracle.md), [GABs de test](gabs-test-balance.md).

## backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/dto/GabCassetteDto.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/dto/GabCassetteDto.java`

```java
package org.intelligentsia.monitoring.domain.monitoring.atm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GabCassetteDto(Integer cassetteNum, String devise, BigDecimal valeurBillet,
        Long billetsRestants, Long billetsRejetes, Long billetsDispenses,
        BigDecimal soldeCassette, LocalDateTime derniereMaj) {}
```

## backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/dto/GabCassettesDto.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/dto/GabCassettesDto.java`

```java
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
```

## backend/src/main/java/org/intelligentsia/monitoring/infrastructure/svfe/repository/GabCassetteRepository.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/java/org/intelligentsia/monitoring/infrastructure/svfe/repository/GabCassetteRepository.java`

```java
package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.domain.monitoring.atm.dto.GabCassetteDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/** Native, read-only Oracle query: no local snapshots or Excel import. */
@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
@Transactional(transactionManager = "svfeTransactionManager", readOnly = true)
public class GabCassetteRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String schemaPrefix;

    public GabCassetteRepository(@Qualifier("svfeJdbcTemplate") NamedParameterJdbcTemplate jdbc,
            @Value("${app.svfe.datasource.schema:}") String schema) {
        this.jdbc = jdbc;
        String normalized = schema.trim();
        if (!normalized.isEmpty() && !normalized.matches("[A-Za-z][A-Za-z0-9_$#]*")) {
            throw new IllegalArgumentException("Schéma Oracle invalide.");
        }
        this.schemaPrefix = normalized.isEmpty() ? "" : normalized + ".";
    }

    public record CassetteRow(Long codeGab, String numeroGab, String nomGab, GabCassetteDto cassette) {}

    public static final RowMapper<CassetteRow> ROW_MAPPER = (rs, rowNum) -> {
        Timestamp date = rs.getTimestamp("DERNIERE_MAJ");
        return new CassetteRow(rs.getObject("CODE_GAB", Long.class), rs.getString("NUMERO_GAB"),
                rs.getString("NOM_GAB"), new GabCassetteDto(rs.getObject("CASSETTE_NUM", Integer.class),
                rs.getString("DEVISE"), rs.getBigDecimal("VALEUR_BILLET"),
                rs.getObject("BILLETS_RESTANTS", Long.class), rs.getObject("BILLETS_REJETES", Long.class),
                rs.getObject("BILLETS_DISPENSES", Long.class), rs.getBigDecimal("SOLDE_CASSETTE"),
                date == null ? null : date.toLocalDateTime()));
    };

    public List<CassetteRow> findLatest(List<Long> codes) {
        // null means all GABs for a global user; an empty authorised set must return no rows.
        if (codes != null && codes.isEmpty()) return List.of();
        String sql = """
                SELECT ATE.ATE_CODE AS CODE_GAB, ATE.ATE_NUM AS NUMERO_GAB,
                       ATE.ATE_LABE AS NOM_GAB, ABD.ABD_CASS AS CASSETTE_NUM,
                       ABD.ABD_CUR_CODE AS DEVISE, ABD.ABD_VALUE AS VALEUR_BILLET,
                       ABD.ABD_REMA_NOTE AS BILLETS_RESTANTS, ABD.ABD_REJE_NOTE AS BILLETS_REJETES,
                       ABD.ABD_DISP_NOTE AS BILLETS_DISPENSES,
                       (ABD.ABD_REMA_NOTE * ABD.ABD_VALUE) AS SOLDE_CASSETTE,
                       ABD.ABD_DATE AS DERNIERE_MAJ
                  FROM %sATM_BALANCE_DETALS ABD
                  INNER JOIN %sATM_TERMINAL ATE ON ABD.ABD_ATE_CODE = ATE.ATE_CODE
                 WHERE (ABD.ABD_ATE_CODE, ABD.ABD_CASS, ABD.ABD_DATE) IN (
                       SELECT ABD2.ABD_ATE_CODE, ABD2.ABD_CASS, MAX(ABD2.ABD_DATE)
                         FROM %sATM_BALANCE_DETALS ABD2
                        GROUP BY ABD2.ABD_ATE_CODE, ABD2.ABD_CASS)
                """.formatted(schemaPrefix, schemaPrefix, schemaPrefix);
        if (codes != null) sql += " AND ATE.ATE_CODE IN (:codes)";
        sql += " ORDER BY ATE.ATE_CODE, ABD.ABD_CASS";
        return jdbc.query(sql, codes == null ? Map.of() : Map.of("codes", codes), ROW_MAPPER);
    }
}
```

## backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/service/GabCassetteService.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/service/GabCassetteService.java`

```java
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
```

## backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/controller/GabCassetteController.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/controller/GabCassetteController.java`

```java
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
```

## frontend/src/services/gabCassetteService.ts

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/frontend/src/services/gabCassetteService.ts`

```typescript
import api from './api'
import type { ApiResponse } from '@/types/api'

export interface GabCassette {
  cassetteNum: number | null
  devise: string | null
  valeurBillet: number | null
  billetsRestants: number | null
  billetsRejetes: number | null
  billetsDispenses: number | null
  soldeCassette: number | null
  derniereMaj: string | null
}
export interface GabCassettes {
  source: 'ORACLE' | 'TEST_EXCEL'
  codeGab: number
  numeroGab: string | null
  nomGab: string | null
  cassettes: GabCassette[]
}
export const gabCassetteService = {
  list: (params: { atmId?: number; institutionId?: number } = {}) =>
    api.get<ApiResponse<GabCassettes[]>>('/gabs/cassettes', { params }).then(response => response.data.data),
}
```

## frontend/src/components/atm/AtmCassettes.tsx

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/frontend/src/components/atm/AtmCassettes.tsx`

```tsx
import { useQuery } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { Banknote, RefreshCw } from 'lucide-react'
import { gabCassetteService } from '@/services/gabCassetteService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { Card } from '@/components/ui/Card'
import { formatDate, formatNumber } from '@/utils/formatters'

export function AtmCassettes({ atmId }: { atmId: number }) {
  const institutionId = useInstitutionFilter()
  const query = useQuery({
    queryKey: ['gab-cassettes', atmId, institutionId],
    queryFn: () => gabCassetteService.list({ atmId, institutionId }),
    enabled: Number.isFinite(atmId) && atmId > 0,
    refetchInterval: 30_000,
    retry: false,
  })
  const gabs = query.data ?? []
  const isTest = gabs.some(gab => gab.source === 'TEST_EXCEL')
  const count = gabs.reduce((total, gab) => total + gab.cassettes.length, 0)
  const error = isAxiosError(query.error) ? query.error.response?.data?.message : undefined
  return <Card className="p-0 overflow-hidden">
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-5 py-4">
      <div className="flex items-center gap-3"><span className="rounded-xl bg-gold-50 p-2.5 text-gold-600"><Banknote className="h-5 w-5" /></span><div><h2 className="font-semibold text-gray-900">Cassettes du GAB</h2><p className="mt-1 text-xs text-gray-500">{isTest ? 'Jeu de test BALANCE.xlsx · valeurs du fichier Excel' : 'Dernier relevé de chaque cassette dans Oracle'}{!query.isPending && !query.isError ? ` · ${count} cassette(s)` : ''}</p></div></div>
      <button type="button" onClick={() => query.refetch()} disabled={query.isFetching} className="flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-600 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${query.isFetching ? 'animate-spin' : ''}`} />Actualiser</button>
    </div>
    {query.isPending ? <p role="status" className="p-8 text-center text-sm text-gray-500">Chargement des cassettes…</p>
      : query.isError ? <div role="alert" className="m-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error ?? 'Impossible de charger les cassettes. Vérifiez la connexion puis réessayez.'}</div>
      : !count ? <p className="p-8 text-center text-sm text-gray-500">Aucun relevé de cassette disponible pour ce GAB.</p>
      : gabs.map(gab => <div key={gab.codeGab}>
        <div className="bg-gray-50/60 px-5 py-3"><h3 className="text-sm font-semibold text-gray-800">{gab.nomGab ?? 'GAB'}</h3><p className="mt-1 text-xs text-gray-500">Code GAB : {gab.codeGab} · Numéro : {gab.numeroGab ?? '—'}</p></div>
        <div className="overflow-x-auto"><table className="w-full min-w-[950px] text-sm"><thead className="bg-gray-50"><tr>{['Cassette n°', 'Devise (code)', 'Valeur du billet', 'Billets restants', 'Rejetés', 'Dispensés', 'Solde cassette', 'Dernière mise à jour'].map((label, index) => <th key={label} className={`px-4 py-3 ${index >= 2 && index <= 6 ? 'text-right' : 'text-left'}`}>{label}</th>)}</tr></thead>
          <tbody>{gab.cassettes.map((cassette, index) => <tr key={`${cassette.cassetteNum}-${index}`} className="border-t border-gray-100"><td className="px-4 py-4 font-semibold">{cassette.cassetteNum ?? '—'}</td><td className="px-4 py-4">{cassette.devise ?? '—'}</td>{[cassette.valeurBillet, cassette.billetsRestants, cassette.billetsRejetes, cassette.billetsDispenses, cassette.soldeCassette].map((value, column) => <td key={column} className={`px-4 py-4 text-right tabular-nums ${column === 4 ? 'font-semibold' : ''}`}>{formatNumber(value)}</td>)}<td className="whitespace-nowrap px-4 py-4 text-gray-500">{formatDate(cassette.derniereMaj, 'dd/MM/yyyy HH:mm:ss')}</td></tr>)}</tbody></table></div>
      </div>)}
  </Card>
}
```

## frontend/src/pages/atm/AtmDetailPage.tsx

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/frontend/src/pages/atm/AtmDetailPage.tsx`

```tsx
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, MapPin, Building2, RefreshCw, Clock, Cpu } from 'lucide-react'
import { atmService } from '@/services/atmService'
import { Card } from '@/components/ui/Card'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { PageLoader } from '@/components/feedback/PageLoader'
import { formatDate } from '@/utils/formatters'
import { atmStatusKey, statusColors } from '@/utils/colorMap'
import { cn } from '@/utils/cn'
import { AtmCassettes } from '@/components/atm/AtmCassettes'
import { AtmMovements } from '@/components/atm/AtmMovements'

// ── Info row ─────────────────────────────────────────────────────────────────

function InfoRow({ label, value, mono }: { label: string; value?: string | null; mono?: boolean }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2.5 border-b border-gray-50 last:border-0">
      <span className="text-sm text-gray-500 shrink-0">{label}</span>
      <span className={cn('text-sm font-medium text-gray-900 text-right', mono && 'font-mono text-xs bg-gray-100 px-2 py-0.5 rounded')}>
        {value ?? '—'}
      </span>
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function AtmDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const atmId = Number(id)

  const { data: atm, isLoading: atmLoading } = useQuery({
    queryKey: ['atm', atmId],
    queryFn: () => atmService.getById(atmId),
  })

  const { data: snapshot, refetch, isFetching } = useQuery({
    queryKey: ['atm-snapshot-latest', atmId],
    queryFn: () => atmService.latestSnapshot(atmId),
    enabled: !!atmId,
    refetchInterval: 30_000,
  })

  if (atmLoading) return <PageLoader />

  const status = atmStatusKey(snapshot)
  const statusDot = statusColors[status]

  return (
    <div className="space-y-6">

      {/* ── Header ── */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="flex h-9 w-9 items-center justify-center rounded-xl border border-gray-200 bg-white text-gray-500 hover:text-gray-800 hover:border-gray-300 transition-all"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-2.5">
            <span
              className="h-10 w-10 rounded-full shrink-0 animate-blink"
              style={{ backgroundColor: statusDot }}
            />
            <h1 className="text-xl font-bold text-gray-900 font-mono">{atm?.terminalId ?? `ATM #${atmId}`}</h1>
            <StatusBadge status={status} className="!text-lg !font-bold !px-2.5 !py-2" />
          </div>
          <p className="text-xs text-gray-400 mt-0.5 ml-5.5">
            {atm?.city && `${atm.city} · `}
            {atm?.institutionName}
          </p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-1.5 text-xs text-gray-500 hover:text-gray-700 transition-colors disabled:opacity-40"
        >
          <RefreshCw className={cn('h-3.5 w-3.5', isFetching && 'animate-spin')} />
          Actualiser
        </button>
      </div>

      <div className="grid grid-cols-1 gap-5">

        {/* ── Info card ── */}
        <Card>
          <div className="flex items-center gap-2 mb-4">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gray-100">
              <Cpu className="h-4 w-4 text-gray-500" />
            </div>
            <h2 className="text-sm font-semibold text-gray-700">Informations</h2>
          </div>
          <InfoRow label="Terminal ID"  value={atm?.terminalId} mono />
          <InfoRow label="Institution"  value={atm?.institutionName} />
          <div className="flex items-start justify-between gap-4 py-2.5 border-b border-gray-50">
            <div className="flex items-center gap-1.5 text-sm text-gray-500 shrink-0">
              <MapPin className="h-3.5 w-3.5" />
              Ville
            </div>
            <span className="text-sm font-medium text-gray-900 text-right">{atm?.city ?? '—'}</span>
          </div>
          <div className="flex items-start justify-between gap-4 py-2.5 border-b border-gray-50">
            <div className="flex items-center gap-1.5 text-sm text-gray-500 shrink-0">
              <Building2 className="h-3.5 w-3.5" />
              Adresse
            </div>
            <span className="text-sm font-medium text-gray-900 text-right max-w-[160px]">{atm?.address ?? '—'}</span>
          </div>
          <div className="flex items-start justify-between gap-4 py-2.5">
            <div className="flex items-center gap-1.5 text-sm text-gray-500 shrink-0">
              <Clock className="h-3.5 w-3.5" />
              Mis à jour
            </div>
            <span className="text-xs font-medium text-gray-900 text-right">{formatDate(atm?.updatedAt)}</span>
          </div>
        </Card>

      </div>
      <AtmCassettes atmId={atmId} />
      {snapshot?.statusMessage && <div className="rounded-xl border border-gray-100 bg-white px-5 py-4 text-sm text-gray-600"><span className="font-medium">Message du GAB : </span>{snapshot.statusMessage}</div>}

      <AtmMovements atmId={atmId} />
    </div>
  )
}
```

## backend/src/main/java/org/intelligentsia/monitoring/config/SecurityConfig.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/java/org/intelligentsia/monitoring/config/SecurityConfig.java`

```java
package org.intelligentsia.monitoring.config;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.infrastructure.security.JwtAuthenticationFilter;
import org.intelligentsia.monitoring.infrastructure.tenant.TenantFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final TenantFilter tenantFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // The SPA refreshes its access token only after a 401. Spring's default
                // entry point returns 403 for anonymous API calls, which leaves an
                // expired persisted session looking logged in while every page is blank.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // TenantFilter runs after JWT so SecurityContext is already populated
                .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class)
                .build();
    }

    /**
     * Prevents Spring Boot from also registering TenantFilter as a plain servlet filter
     * (it's already in the Spring Security chain above).
     */
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

## backend/src/main/resources/application.yml

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: monitoring-backend

  datasource:
    # The local app is expected to show the explicit TEST dataset. Production
    # keeps its separate URL in application-prod.yml or DB_URL.
    url: ${DB_URL:jdbc:postgresql://localhost:5432/monitoring_test}
    username: ${DB_USER:neo-monitoring}
    password: ${DB_PASS:monitoring123}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        default_schema: public

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    username: ${MAIL_USER:}
    password: ${MAIL_PASS:}
    properties:
      mail:
        smtp:
          auth: ${MAIL_AUTH:true}
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
          starttls:
            enable: ${MAIL_STARTTLS:true}

  thymeleaf:
    check-template-location: true
    prefix: classpath:/templates/
    suffix: .html
    encoding: UTF-8

  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
    time-zone: Africa/Abidjan

server:
  port: 8089
  servlet:
    context-path: /
  error:
    include-message: never
    include-stacktrace: never

app:
  jwt:
    secret: ${JWT_SECRET:devsecretkeychangeinproductionmustbeatleast512bitslongbase64}
    access-expiry-ms: 900000       # 15 minutes
    refresh-expiry-ms: 604800000   # 7 jours

  mail:
    from: ${MAIL_FROM:noreply@monitoring.local}

  notifications:
    alert-email:
      enabled: ${ALERT_EMAIL_NOTIFICATIONS_ENABLED:true}

  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000}

  svfe:
    enabled: ${SVFE_ENABLED:false}
    datasource:
      schema: ${SVFE_DB_SCHEMA:}
      url: ${SVFE_DB_URL:}
      username: ${SVFE_DB_USER:}
      password: ${SVFE_DB_PASS:}

  settings:
    encryption-key: ${SETTINGS_ENCRYPTION_KEY:changeThisKeyInProd32CharsMinimum!}
    encryption-salt: ${SETTINGS_ENCRYPTION_SALT:deadbeefcafebabe}

  scheduling:
    enabled: true
    retention-days: 90

  pagination:
    default-page-size: 20
    max-page-size: 100

  extraction:
    output-dir: ${EXTRACTION_OUTPUT_DIR:${java.io.tmpdir}/extractions}
    max-rows: ${EXTRACTION_MAX_ROWS:500000}   # hard limit per job to prevent OOM

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

  health:
    mail:
      enabled: false

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: alpha
    tags-sorter: alpha
  info:
    title: Monitoring Monétique API
    description: API REST de la plateforme de surveillance du réseau monétique
    version: 1.0.0
```

## backend/src/test/java/org/intelligentsia/monitoring/domain/monitoring/atm/service/GabCassetteServiceTest.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/test/java/org/intelligentsia/monitoring/domain/monitoring/atm/service/GabCassetteServiceTest.java`

```java
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
```

## backend/src/test/java/org/intelligentsia/monitoring/infrastructure/svfe/repository/GabCassetteRepositoryTest.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/test/java/org/intelligentsia/monitoring/infrastructure/svfe/repository/GabCassetteRepositoryTest.java`

```java
package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GabCassetteRepositoryTest {
    @Test void jdbcNullsStayNullInJsonAndDateStaysIso() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("CODE_GAB", Long.class)).thenReturn(1067780L);
        when(rs.getString("NUMERO_GAB")).thenReturn("00108401001");
        when(rs.getTimestamp("DERNIERE_MAJ")).thenReturn(Timestamp.valueOf("2026-04-11 11:58:01"));
        var row = GabCassetteRepository.ROW_MAPPER.mapRow(rs, 0);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var json = mapper.readTree(mapper.writeValueAsString(row.cassette()));
        assertTrue(json.has("billetsRestants")); assertTrue(json.get("billetsRestants").isNull());
        assertTrue(json.get("soldeCassette").isNull());
        assertEquals("2026-04-11T11:58:01", json.get("derniereMaj").asText());
        assertEquals("00108401001", row.numeroGab());
    }
    @Test void rejectsSqlInjectionInSchemaConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new GabCassetteRepository(mock(NamedParameterJdbcTemplate.class), "MXP;DROP TABLE ATM_TERMINAL"));
    }
    @Test @EnabledIfSystemProperty(named = "monitoring.dbTests", matches = "true")
    void nativeSqlPicksLatestForEachCassetteAndPreservesNulls() throws Exception {
        // Disposable temporary tables only; PostgreSQL checks the portable tuple/MAX query,
        // not Oracle connectivity or Oracle-specific driver behaviour.
        try (Connection connection = DriverManager.getConnection(System.getProperty("monitoring.testJdbcUrl", "jdbc:postgresql://localhost:5432/monitoring_test"),
                System.getProperty("monitoring.testDbUser", "neo-monitoring"), System.getProperty("monitoring.testDbPassword", "monitoring123"))) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TEMP TABLE ATM_TERMINAL (ATE_CODE BIGINT, ATE_NUM VARCHAR(50), ATE_LABE VARCHAR(100)) ON COMMIT DROP");
                statement.execute("CREATE TEMP TABLE ATM_BALANCE_DETALS (ABD_ATE_CODE BIGINT, ABD_CASS INTEGER, ABD_CUR_CODE INTEGER, ABD_VALUE NUMERIC, ABD_REMA_NOTE BIGINT, ABD_REJE_NOTE BIGINT, ABD_DISP_NOTE BIGINT, ABD_DATE TIMESTAMP) ON COMMIT DROP");
                statement.execute("INSERT INTO ATM_TERMINAL VALUES (1, '0001', 'GAB Un'), (2, '0002', 'GAB Deux')");
                statement.execute("INSERT INTO ATM_BALANCE_DETALS VALUES (1,1,28,100,244,9,47,'2025-06-21 08:44:35'), (1,1,28,100,163,6,31,'2026-04-11 11:58:01'), (1,2,28,100,NULL,NULL,NULL,'2025-06-18 14:36:21'), (2,1,28,100,0,0,0,'2026-04-10 10:00:00')");
                var jdbc = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));
                var repository = new GabCassetteRepository(jdbc, "");
                var rows = repository.findLatest(null);
                assertEquals(3, rows.size()); assertEquals(163L, rows.get(0).cassette().billetsRestants());
                assertEquals(16300, rows.get(0).cassette().soldeCassette().intValueExact());
                assertEquals("2026-04-11T11:58:01", rows.get(0).cassette().derniereMaj().toString());
                assertNull(rows.get(1).cassette().billetsRestants()); assertNull(rows.get(1).cassette().soldeCassette());
                assertEquals(0L, rows.get(2).cassette().billetsRestants());
                assertEquals(2, repository.findLatest(List.of(1L)).size());
                assertTrue(repository.findLatest(List.of()).isEmpty());
            } finally { connection.rollback(); }
        }
    }
}
```

## backend/src/test/java/org/intelligentsia/monitoring/domain/monitoring/atm/service/MonitoringEndpointsTest.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/test/java/org/intelligentsia/monitoring/domain/monitoring/atm/service/MonitoringEndpointsTest.java`

```java
package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.extraction.service.ExtractionSchedulerService;
import org.intelligentsia.monitoring.infrastructure.scheduler.MonitoringScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@EnabledIfSystemProperty(named = "monitoring.dbTests", matches = "true")
@SpringBootTest(properties = {"spring.datasource.url=jdbc:postgresql://localhost:5432/monitoring_test", "app.svfe.enabled=false"})
@AutoConfigureMockMvc
class MonitoringEndpointsTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.intelligentsia.monitoring.domain.configuration.settings.service.MailConfigService mailConfig;
    @MockitoBean MonitoringScheduler scheduler;
    @MockitoBean ExtractionSchedulerService extractionScheduler;

    @Test
    @EnabledIfSystemProperty(named = "monitoring.smtpTest", matches = "true")
    void authenticatesUsingDatabaseSmtpSettingsWithoutSendingMail() throws Exception {
        ((org.springframework.mail.javamail.JavaMailSenderImpl) mailConfig.buildSender()).testConnection();
    }

    @Test
    void rolesAreVisibleWithPermissionAndDeniedWithoutIt() throws Exception {
        Integer grant = jdbc.queryForObject("""
                SELECT COUNT(*) FROM role_permissions rp JOIN roles r ON r.id=rp.role_id
                JOIN permissions p ON p.id=rp.permission_id WHERE r.code='ROLE_ADMIN' AND p.code='ROLE_READ'
                """, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, grant);
        var reader = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_READ"))));
        mvc.perform(get("/api/roles").with(reader)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.code == 'ROLE_ADMIN')].permissions[0]").exists());
        var nonReader = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test", null,
                java.util.List.of(new SimpleGrantedAuthority("ATM_READ"))));
        mvc.perform(get("/api/roles").with(nonReader)).andExpect(status().isForbidden());
    }

    @Test
    void retiredCombinedRuleIsRemovedFromCatalogueWhileKeepingItsHistory() throws Exception {
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test", null,
                java.util.List.of(new SimpleGrantedAuthority("MONITORING_RULE_VIEW"))));
        for (String category : new String[]{"", "ATM"}) {
            var response = mvc.perform(get("/api/monitoring/rules").param("category", category).with(auth))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertFalse(response.contains("RULE_ATM_INTERFACE_UNAVAILABLE_5X"));
            org.junit.jupiter.api.Assertions.assertTrue(response.contains("RULE_ATM_INTERFACE_DOWN_5X"));
            org.junit.jupiter.api.Assertions.assertTrue(response.contains("RULE_ATM_INTERFACE_SIGNOFF_5X"));
        }
        var legacy = jdbc.queryForList("SELECT id FROM monitoring_rules WHERE code='RULE_ATM_INTERFACE_UNAVAILABLE_5X'", Long.class);
        if (!legacy.isEmpty()) {
            mvc.perform(get("/api/monitoring/rules/{id}", legacy.get(0)).with(auth)).andExpect(status().isNotFound());
        }
    }

    @Test
    void servesMovementsExportsAndNetworkFiltersOnTheRealSchema() throws Exception {
        var principal = new User(); principal.setId(1L); principal.setLogin("test"); principal.setScope(UserScope.ADMIN);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                java.util.List.of(new SimpleGrantedAuthority("ATM_READ"), new SimpleGrantedAuthority("TRANSACTION_READ"), new SimpleGrantedAuthority("NETWORK_READ"))));
        Long id = jdbc.queryForObject("SELECT MIN(id) FROM atms", Long.class);
        mvc.perform(get("/api/atms/{id}/movements", id).with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
        mvc.perform(get("/api/atms/{id}/movements", id).param("status", "DOWN").with(auth)).andExpect(status().isOk());
        mvc.perform(get("/api/atms/{id}/movements", id).param("institutionId", "-1").with(auth)).andExpect(status().isNotFound());
        for (String format : new String[]{"PDF", "WORD", "EXCEL"}) {
            mvc.perform(get("/api/atms/{id}/movements/export", id).param("format", format).with(auth))
                    .andExpect(status().isOk()).andExpect(header().exists("Content-Disposition"));
            mvc.perform(get("/api/atms/report").param("format", format).param("search", "TEST").with(auth)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/atms/report").param("format", "INVALID").with(auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/network/latest").with(auth)).andExpect(status().isOk());
        mvc.perform(get("/api/transactions/networks").with(auth)).andExpect(status().isOk());
        for (String endpoint : new String[]{"rates", "errors"}) {
            mvc.perform(get("/api/transactions/" + endpoint).with(auth)).andExpect(status().isOk());
            mvc.perform(get("/api/transactions/" + endpoint).param("network", "NO_SUCH_NETWORK").param("state", "UP").with(auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
            mvc.perform(get("/api/transactions/" + endpoint).param("institutionId", "-1").with(auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Test void cassetteApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/gabs/cassettes")).andExpect(status().isUnauthorized());
    }
    @Test void cassetteApiRequiresAtmReadPermission() throws Exception {
        User user = new User(); user.setId(-1L); user.setScope(UserScope.GLOBAL);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        mvc.perform(get("/api/gabs/cassettes").with(auth)).andExpect(status().isForbidden());
    }
    @Test void disabledOracleIsReportedByCassetteApi() throws Exception {
        User user = new User(); user.setId(-1L); user.setScope(UserScope.GLOBAL);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, java.util.List.of(new SimpleGrantedAuthority("ATM_READ"))));
        mvc.perform(get("/api/gabs/cassettes").with(auth)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ORACLE_CASSETTES_UNAVAILABLE"));
    }
    @Test void cassetteCorsPreflightAllowsFrontendAndRejectsOtherOrigins() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/gabs/cassettes")
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/gabs/cassettes")
                .header("Origin", "https://untrusted.invalid").header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void explicitExcelFixtureIsVisibleOnlyInItsInstitutionAndDeletesWithGab() throws Exception {
        Long institutionId = jdbc.queryForObject("SELECT id FROM institutions ORDER BY id LIMIT 1", Long.class);
        Long atmId = jdbc.queryForObject("INSERT INTO atms(institution_id,terminal_id) VALUES (?,?) RETURNING id", Long.class,
                institutionId, "TEST-CASSETTE-" + java.util.UUID.randomUUID());
        String payload = """
                {"codeGab":1067780,"numeroGab":"108401001","nomGab":"GAB Excel",
                 "source":"TEST_EXCEL","cassettes":[{"cassetteNum":1,"devise":"28",
                 "valeurBillet":100,"billetsRestants":null,"billetsRejetes":0,
                 "billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2025-06-21T08:44:35"}]}
                """;
        jdbc.update("INSERT INTO atm_test_cassette_fixtures(atm_id,dataset_code,payload) VALUES (?, 'JUNIT_EXCEL', ?::jsonb)", atmId, payload);
        var institution = new org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution();
        institution.setId(institutionId);
        User reader = new User(); reader.setId(-1L); reader.setScope(UserScope.INSTITUTION); reader.setInstitution(institution);
        var auth = authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(reader, null,
                java.util.List.of(new SimpleGrantedAuthority("ATM_READ"))));
        mvc.perform(get("/api/gabs/cassettes").param("atmId", atmId.toString()).param("institutionId", "-1").with(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].source").value("TEST_EXCEL"))
                .andExpect(jsonPath("$.data[0].cassettes[0].billetsRestants").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data[0].cassettes[0].billetsRejetes").value(0))
                .andExpect(jsonPath("$.data[0].cassettes[0].derniereMaj").value("2025-06-21T08:44:35"));
        institution.setId(-1L);
        mvc.perform(get("/api/gabs/cassettes").param("atmId", atmId.toString()).with(auth)).andExpect(status().isNotFound());
        jdbc.update("DELETE FROM atms WHERE id=?", atmId);
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM atm_test_cassette_fixtures WHERE atm_id=?", Integer.class, atmId));
    }
}
```

## backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/repository/GabCassetteFixtureRepository.java

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/java/org/intelligentsia/monitoring/domain/monitoring/atm/repository/GabCassetteFixtureRepository.java`

```java
package org.intelligentsia.monitoring.domain.monitoring.atm.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.GabCassettesDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/** Only explicitly registered Excel test GABs use this local fixture source. */
@Repository
public class GabCassetteFixtureRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public GabCassetteFixtureRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc; this.mapper = mapper;
    }
    public Optional<GabCassettesDto> findByAtmId(Long atmId) {
        return jdbc.query("SELECT payload::text FROM atm_test_cassette_fixtures WHERE atm_id = ?", (rs, row) -> {
            try { return mapper.readValue(rs.getString(1), GabCassettesDto.class); }
            catch (JsonProcessingException ex) { throw new IllegalStateException("Jeu de cassettes de test invalide.", ex); }
        }, atmId).stream().findFirst();
    }
}
```

## backend/src/main/resources/db/migration/V31__atm_test_cassette_fixtures.sql

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/src/main/resources/db/migration/V31__atm_test_cassette_fixtures.sql`

```sql
-- Explicit temporary test datasets, isolated from Oracle monitoring snapshots.
-- No fixtures are seeded by Flyway on any environment.
CREATE TABLE atm_test_cassette_fixtures (
    atm_id BIGINT PRIMARY KEY REFERENCES atms(id) ON DELETE CASCADE,
    dataset_code VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT atm_test_cassette_source_check CHECK (payload->>'source' = 'TEST_EXCEL'),
    CONSTRAINT atm_test_cassette_array_check CHECK (jsonb_typeof(payload->'cassettes') = 'array')
);
CREATE INDEX idx_atm_test_cassette_dataset ON atm_test_cassette_fixtures(dataset_code);
```

## backend/scripts/test-data/seed-balance-gabs.sql

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/scripts/test-data/seed-balance-gabs.sql`

```sql
BEGIN;
DO $$ BEGIN IF current_database() <> 'monitoring_test' THEN RAISE EXCEPTION 'Ce jeu est réservé à monitoring_test'; END IF; IF (SELECT COUNT(*) FROM institutions WHERE code IN ('00001','00043')) <> 2 THEN RAISE EXCEPTION 'Les deux institutions de test sont requises'; END IF; END $$;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401001' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401001','[TEST BALANCE.xlsx] GAB N1 HAI CINEMA',NULL,NULL FROM institutions WHERE code='00043' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1067780,"numeroGab":"108401001","nomGab":"GAB N1 HAI CINEMA","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":244,"billetsRejetes":9,"billetsDispenses":47,"soldeCassette":24400,"derniereMaj":"2025-06-21T08:44:35"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":163,"billetsRejetes":6,"billetsDispenses":31,"soldeCassette":16300,"derniereMaj":"2025-06-21T08:44:35"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":171,"billetsRejetes":25,"billetsDispenses":4,"soldeCassette":17100,"derniereMaj":"2025-06-18T14:36:21"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":161,"billetsRejetes":29,"billetsDispenses":10,"soldeCassette":16100,"derniereMaj":"2025-06-18T14:36:21"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401001' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401002' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401002','[TEST BALANCE.xlsx] GAB N2 HAI CINEMA',NULL,NULL FROM institutions WHERE code='00043' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1069780,"numeroGab":"108401002","nomGab":"GAB N2 HAI CINEMA","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":375,"billetsRejetes":1,"billetsDispenses":124,"soldeCassette":37500,"derniereMaj":"2026-04-11T11:58:01"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":314,"billetsRejetes":1,"billetsDispenses":85,"soldeCassette":31400,"derniereMaj":"2026-04-11T11:58:01"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-28T18:18:42"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-28T18:18:42"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401002' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401003' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401003','[TEST BALANCE.xlsx] GAB N1 AIRPORT',NULL,NULL FROM institutions WHERE code='00001' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1069820,"numeroGab":"108401003","nomGab":"GAB N1 AIRPORT","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":281,"billetsRejetes":1,"billetsDispenses":18,"soldeCassette":28100,"derniereMaj":"2026-04-11T11:58:02"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2024-11-05T17:33:20"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2024-06-11T10:14:54"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2024-06-11T10:14:54"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401003' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401004' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401004','[TEST BALANCE.xlsx] ATM N1 SHERIKAT',NULL,NULL FROM institutions WHERE code='00043' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1074200,"numeroGab":"108401004","nomGab":"ATM N1 SHERIKAT","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2024-08-23T11:59:38"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2024-08-23T11:59:38"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2024-08-23T11:59:38"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2024-08-23T11:59:39"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401004' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401005' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401005','[TEST BALANCE.xlsx] ATM N2 SHERIKAT',NULL,NULL FROM institutions WHERE code='00001' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1074220,"numeroGab":"108401005","nomGab":"ATM N2 SHERIKAT","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":91,"billetsRejetes":1,"billetsDispenses":108,"soldeCassette":9100,"derniereMaj":"2026-04-10T16:53:39"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-03T11:50:19"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-03T11:50:19"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-03T11:50:19"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401005' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401006' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401006','[TEST BALANCE.xlsx] ATM N3 SHERIKAT',NULL,NULL FROM institutions WHERE code='00043' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1074240,"numeroGab":"108401006","nomGab":"ATM N3 SHERIKAT","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":184,"billetsRejetes":2,"billetsDispenses":14,"soldeCassette":18400,"derniereMaj":"2025-06-18T14:55:06"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-06T11:15:52"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2024-12-07T09:15:25"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2024-12-07T09:15:25"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401006' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401007' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401007','[TEST BALANCE.xlsx] ATM GUDELE 21',NULL,NULL FROM institutions WHERE code='00001' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1078200,"numeroGab":"108401007","nomGab":"ATM GUDELE 21","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":130,"billetsRejetes":1,"billetsDispenses":69,"soldeCassette":13000,"derniereMaj":"2026-04-10T09:26:54"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-20T14:15:01"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-20T14:15:02"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-20T14:15:02"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401007' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401008' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401008','[TEST BALANCE.xlsx] ATM GUDELE 22',NULL,NULL FROM institutions WHERE code='00043' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1078600,"numeroGab":"108401008","nomGab":"ATM GUDELE 22","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2025-03-26T12:37:56"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2025-03-26T12:37:56"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2025-03-26T12:37:56"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":null,"billetsRejetes":null,"billetsDispenses":null,"soldeCassette":null,"derniereMaj":"2025-03-26T12:37:56"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00043' AND a.terminal_id='TEST-BALANCE-108401008' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401009' AND NOT EXISTS(SELECT 1 FROM atm_test_cassette_fixtures f WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012')) THEN RAISE EXCEPTION 'Un GAB existant non lié au jeu occupe ce numéro'; END IF; END $$;
INSERT INTO atms (institution_id,terminal_id,address,city,svfe_pid) SELECT id,'TEST-BALANCE-108401009','[TEST BALANCE.xlsx] ATM KONYO KONYO',NULL,NULL FROM institutions WHERE code='00001' ON CONFLICT (institution_id,terminal_id) DO NOTHING;
INSERT INTO atm_test_cassette_fixtures (atm_id,dataset_code,payload) SELECT a.id,'BALANCE_XLSX_20261012','{"codeGab":1083800,"numeroGab":"108401009","nomGab":"ATM KONYO KONYO","cassettes":[{"cassetteNum":1,"devise":"28","valeurBillet":100,"billetsRestants":26,"billetsRejetes":2,"billetsDispenses":72,"soldeCassette":2600,"derniereMaj":"2026-04-07T17:34:07"},{"cassetteNum":2,"devise":"28","valeurBillet":100,"billetsRestants":26,"billetsRejetes":6,"billetsDispenses":68,"soldeCassette":2600,"derniereMaj":"2026-04-08T15:58:53"},{"cassetteNum":3,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-25T16:22:13"},{"cassetteNum":4,"devise":"28","valeurBillet":100,"billetsRestants":0,"billetsRejetes":0,"billetsDispenses":0,"soldeCassette":0,"derniereMaj":"2025-06-25T16:22:13"}],"source":"TEST_EXCEL"}'::jsonb FROM atms a JOIN institutions i ON i.id=a.institution_id WHERE i.code='00001' AND a.terminal_id='TEST-BALANCE-108401009' ON CONFLICT (atm_id) DO UPDATE SET payload=EXCLUDED.payload WHERE atm_test_cassette_fixtures.dataset_code=EXCLUDED.dataset_code;
DO $$ BEGIN IF (SELECT COUNT(*) FROM atm_test_cassette_fixtures WHERE dataset_code='BALANCE_XLSX_20261012') <> 9 THEN RAISE EXCEPTION '9 GABs attendus'; END IF; IF (SELECT SUM(jsonb_array_length(payload->'cassettes')) FROM atm_test_cassette_fixtures WHERE dataset_code='BALANCE_XLSX_20261012') <> 36 THEN RAISE EXCEPTION '36 cassettes attendues'; END IF; END $$;
COMMIT;
```

## backend/scripts/test-data/remove-balance-gabs.sql

Chemin : `C:/Users/IT SOLUTION SARL/Intelligentsia/application-monitoring-intelligentsia-main/backend/scripts/test-data/remove-balance-gabs.sql`

```sql
-- À exécuter seulement quand la suppression du jeu est demandée.
BEGIN;
DO $$ BEGIN IF current_database() <> 'monitoring_test' THEN RAISE EXCEPTION 'Base de test requise'; END IF; END $$;
DELETE FROM atms a USING atm_test_cassette_fixtures f
WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012'
  AND a.terminal_id LIKE 'TEST-BALANCE-%';
-- Les cassettes du jeu sont supprimées par ON DELETE CASCADE.
COMMIT;
```
