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
