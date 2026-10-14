package org.intelligentsia.monitoring.domain.monitoring.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.AtmSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.dashboard.dto.DashboardSummaryDto;
import org.intelligentsia.monitoring.domain.monitoring.host.dto.HostSnapshotDto;
import org.intelligentsia.monitoring.domain.monitoring.host.entity.HostSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.host.repository.HostSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.network.dto.NetworkSnapshotDto;
import org.intelligentsia.monitoring.domain.monitoring.network.entity.NetworkSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.network.repository.NetworkSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.pos.entity.PosTelecollectSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.pos.repository.PosTelecollectSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.transaction.entity.TransactionRate;
import org.intelligentsia.monitoring.domain.monitoring.transaction.repository.TransactionRateRepository;
import org.intelligentsia.monitoring.domain.monitoring.dashboard.dto.DashboardHistoryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AtmSnapshotRepository atmSnapshotRepo;
    private final NetworkSnapshotRepository networkSnapshotRepo;
    private final HostSnapshotRepository hostSnapshotRepo;
    private final TransactionRateRepository txnRateRepo;
    private final NamedParameterJdbcTemplate jdbc;
    private final PosTelecollectSnapshotRepository posTelecollectRepo;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(Long institutionId) {

        // ── ATMs ──────────────────────────────────────────────────────────────
        var atmCounts = jdbc.queryForList("SELECT status_key,COUNT(*) AS count FROM atm_display_status WHERE (CAST(:institutionId AS bigint) IS NULL OR institution_id=:institutionId) GROUP BY status_key", new MapSqlParameterSource("institutionId", institutionId));
        int totalAtms=0, atmsOffline=0, atmsWarning=0, atmsOnline=0, atmsUnknown=0;
        for (var row : atmCounts) {
            int count=((Number)row.get("count")).intValue(); totalAtms+=count;
            switch (row.get("status_key").toString()) { case "ok" -> atmsOnline=count; case "warning" -> atmsWarning=count; case "error" -> atmsOffline=count; default -> atmsUnknown=count; }
        }

        List<NetworkSnapshotDto> networks = (institutionId == null ? networkSnapshotRepo.findAllLatest() : List.<NetworkSnapshot>of()).stream()
                .map(this::toNetworkDto)
                .collect(Collectors.toList());

        // ── Hosts ─────────────────────────────────────────────────────────────
        List<HostSnapshotDto> hosts = (institutionId == null ? hostSnapshotRepo.findAllLatest() : List.<HostSnapshot>of()).stream()
                .map(this::toHostDto)
                .collect(Collectors.toList());

        // ── Transaction success rate ───────────────────────────────────────────
        List<TransactionRate> latestRates = institutionId == null ? txnRateRepo.findAllLatest() : List.of();
        long totalSuccess = latestRates.stream()
                .mapToLong(r -> r.getSuccessCount() != null ? r.getSuccessCount() : 0).sum();
        long totalAll = latestRates.stream().mapToLong(r ->
                (r.getSuccessCount() != null ? r.getSuccessCount() : 0)
                + (r.getFailureCount() != null ? r.getFailureCount() : 0)).sum();
        // Match the Réseaux page: use the latest collected window and calculate
        // a volume-weighted rate. Keep the last known value if collection pauses.
        Double transactionSuccessRate = totalAll > 0 ? totalSuccess * 100.0 / totalAll : null;

        // ── GAB-linked alerts, regardless of workflow status, rolling 24 h ─
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fraudSince = now.minusHours(24);
        MapSqlParameterSource fraudScope = new MapSqlParameterSource("institutionId", institutionId);
        int gabAlertsToday = Math.toIntExact(jdbc.queryForObject("""
                SELECT COUNT(*) FROM monitoring_alerts a
                LEFT JOIN monitoring_rules r ON r.id = a.rule_id
                LEFT JOIN atms atm ON atm.id = a.atm_id
                WHERE (a.category = 'ATM' OR r.category = 'ATM' OR a.atm_id IS NOT NULL
                       OR r.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X'))
                  AND a.deleted_at IS NULL
                  AND a.detected_at >= :fraudSince AND a.detected_at < :now
                  AND (CAST(:institutionId AS BIGINT) IS NULL
                       OR COALESCE(a.institution_id, atm.institution_id) = :institutionId)
                """, fraudScope.addValue("fraudSince", fraudSince).addValue("now", now), Long.class));
        int totalFrauds = Math.toIntExact(jdbc.queryForObject("""
                SELECT COUNT(*) FROM monitoring_alerts a
                LEFT JOIN atms atm ON atm.id = a.atm_id
                WHERE a.category = 'FRAUD' AND a.deleted_at IS NULL
                  AND (CAST(:institutionId AS BIGINT) IS NULL
                       OR COALESCE(a.institution_id, atm.institution_id) = :institutionId)
                """, fraudScope, Long.class));

        // ── POS telecollect ───────────────────────────────────────────────────
        List<PosTelecollectSnapshot> latestPos = posTelecollectRepo.findLatestByInstitution(institutionId);
        int totalPos             = latestPos.size();
        int posWithoutTelecollect = (int) latestPos.stream().filter(s -> "NON_OK".equals(s.getStatus())).count();

        return DashboardSummaryDto.builder()
                .totalAtms(totalAtms)
                .atmsOnline(Math.max(atmsOnline, 0))
                .atmsOffline(atmsOffline)
                .atmsWarning(atmsWarning).atmsUnknown(atmsUnknown)
                .totalPos(totalPos)
                .posWithoutTelecollect(posWithoutTelecollect)
                .gabAlertsToday(gabAlertsToday)
                .totalFrauds(totalFrauds)
                .transactionSuccessRate(transactionSuccessRate)
                .networks(networks)
                .hosts(hosts)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardHistoryDto getHistory(Long institutionId) {

        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        List<AtmSnapshot> atmSnapshots =
                atmSnapshotRepo.findHistory(institutionId, since);

        /*
        * GAB :
        * On regroupe les snapshots par heure.
        *
        * Un GAB est considéré actif lorsqu'il est :
        * - en ligne
        * - ou en avertissement
        *
        * Il est inactif lorsqu'il est hors ligne.
        */
        ZoneId zone = ZoneId.systemDefault();
        Map<Instant, List<AtmSnapshot>> atmByHour =
                atmSnapshots.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getSnapshotTime()
                                        .atZone(zone)
                                        .truncatedTo(ChronoUnit.HOURS)
                                        .toInstant(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<DashboardHistoryDto.AtmAvailabilityPoint> atmHistory =
                atmByHour.entrySet().stream()
                        .map(entry -> {

                            List<AtmSnapshot> snapshots = entry.getValue();

                            long active = snapshots.stream()
                                    .filter(s -> List.of("4", "SIGNON", "SIGN_ON", "OK", "ONLINE").contains(s.getStatusId() == null ? "" : s.getStatusId().trim().toUpperCase(java.util.Locale.ROOT)))
                                    .count();

                            long total = snapshots.size();

                            double percentage =
                                    total > 0
                                            ? (active * 100.0) / total
                                            : 0.0;

                            return DashboardHistoryDto.AtmAvailabilityPoint
                                    .builder()
                                    .time(entry.getKey())
                                    .percentage(Math.round(percentage * 10.0) / 10.0)
                                    .build();
                        })
                        .toList();

        LocalDateTime firstHour = LocalDateTime.now(zone).truncatedTo(ChronoUnit.HOURS).minusHours(23);
        LocalDateTime fraudSince = LocalDateTime.ofInstant(since, zone);
        Map<LocalDateTime, Integer> fraudsByHour = jdbc.query("""
                SELECT date_trunc('hour', a.detected_at) AS hour_bucket, COUNT(*) AS total
                FROM monitoring_alerts a
                WHERE a.category = 'FRAUD' AND a.deleted_at IS NULL
                  AND a.detected_at >= :since
                  AND (CAST(:institutionId AS BIGINT) IS NULL OR a.institution_id = :institutionId)
                GROUP BY 1 ORDER BY 1
                """, new MapSqlParameterSource("since", fraudSince).addValue("institutionId", institutionId), rs -> {
            Map<LocalDateTime, Integer> counts = new LinkedHashMap<>();
            while (rs.next()) {
                counts.put(rs.getTimestamp("hour_bucket").toLocalDateTime(), rs.getInt("total"));
            }
            return counts;
        });
        List<DashboardHistoryDto.FraudHistoryPoint> fraudHistory = java.util.stream.IntStream.range(0, 24)
                .mapToObj(offset -> {
                    LocalDateTime hour = firstHour.plusHours(offset);
                    return DashboardHistoryDto.FraudHistoryPoint.builder()
                            .time(hour.atZone(zone).toInstant())
                            .count(fraudsByHour.getOrDefault(hour, 0))
                            .build();
                })
                .toList();

        List<AtmAlertBucket> atmAlertBuckets = jdbc.query("""
                SELECT date_trunc('hour', a.detected_at) AS hour_bucket,
                       COALESCE(atm.terminal_id,
                                NULLIF(substring(a.description from 'Identifiant[[:space:]]*:[[:space:]]*([^[:cntrl:]]+)'), ''),
                                NULLIF(TRIM(SUBSTRING(a.title FROM POSITION('—' IN a.title) + 1)), ''),
                                CONCAT('GAB #', COALESCE(a.atm_id, a.id))) AS terminal_id,
                       COUNT(*) AS total
                FROM monitoring_alerts a
                JOIN monitoring_rules r ON r.id = a.rule_id
                LEFT JOIN atms atm ON atm.id = a.atm_id
                WHERE (a.category = 'ATM' OR r.category = 'ATM' OR a.atm_id IS NOT NULL
                       OR r.code IN ('RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X'))
                  AND a.deleted_at IS NULL
                  AND a.detected_at >= :since
                  AND (CAST(:institutionId AS BIGINT) IS NULL
                       OR COALESCE(a.institution_id, atm.institution_id) = :institutionId)
                GROUP BY 1, 2 ORDER BY 1, 2
                """, new MapSqlParameterSource("since", fraudSince).addValue("institutionId", institutionId), (rs, row) ->
                new AtmAlertBucket(rs.getTimestamp("hour_bucket").toLocalDateTime(), rs.getString("terminal_id"), rs.getInt("total")));
        List<String> topAtms = atmAlertBuckets.stream()
                .collect(Collectors.groupingBy(AtmAlertBucket::terminalId, Collectors.summingInt(AtmAlertBucket::count)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        Map<String, Map<LocalDateTime, Integer>> atmCountsByHour = new LinkedHashMap<>();
        for (AtmAlertBucket bucket : atmAlertBuckets) {
            if (topAtms.contains(bucket.terminalId())) {
                atmCountsByHour.computeIfAbsent(bucket.terminalId(), ignored -> new LinkedHashMap<>())
                        .put(bucket.hour(), bucket.count());
            }
        }
        List<DashboardHistoryDto.AtmAlertImpactPoint> atmAlertImpact = topAtms.stream()
                .flatMap(terminalId -> java.util.stream.IntStream.range(0, 24)
                        .mapToObj(offset -> {
                            LocalDateTime hour = firstHour.plusHours(offset);
                            return DashboardHistoryDto.AtmAlertImpactPoint.builder()
                                    .time(hour.atZone(zone).toInstant())
                                    .terminalId(terminalId)
                                    .count(atmCountsByHour.getOrDefault(terminalId, Map.of()).getOrDefault(hour, 0))
                                    .build();
                        }))
                .toList();

        return DashboardHistoryDto.builder()
                .atmAvailabilityHistory(atmHistory)
                .atmAlertImpact(atmAlertImpact)
                .fraudHistory(fraudHistory)
                .build();
    }

    private record AtmAlertBucket(LocalDateTime hour, String terminalId, int count) {}

    private NetworkSnapshotDto toNetworkDto(NetworkSnapshot s) {
        NetworkSnapshotDto dto = new NetworkSnapshotDto();
        dto.setId(s.getId());
        dto.setNetworkCode(s.getNetworkCode());
        dto.setDescription(s.getDescription());
        dto.setOperatingStatus(s.getOperatingStatus());
        dto.setProcessingStatus(s.getProcessingStatus());
        dto.setTransactionMode(s.getTransactionMode());
        dto.setSnapshotTime(s.getSnapshotTime());
        return dto;
    }

    private HostSnapshotDto toHostDto(HostSnapshot s) {
        HostSnapshotDto dto = new HostSnapshotDto();
        dto.setId(s.getId());
        dto.setHostname(s.getHostname());
        dto.setDescription(s.getDescription());
        dto.setIpAddress(s.getIpAddress());
        dto.setOperatingStatus(s.getOperatingStatus());
        dto.setProcessingStatus(s.getProcessingStatus());
        dto.setSnapshotTime(s.getSnapshotTime());
        return dto;
    }
}
