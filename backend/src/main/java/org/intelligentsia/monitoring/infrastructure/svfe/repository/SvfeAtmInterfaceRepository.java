package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** Reads the full interface row and its status label from the SVFE Oracle schema. */
@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public class SvfeAtmInterfaceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SvfeAtmInterfaceRepository(@Qualifier("svfeJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT = """
            SELECT I.INT_CODE, I.INT_IDEN, I.INT_LABE, I.INT_STAT,
                   I.INT_LAST_UP_TIME, I.INT_LAST_DOWN_TIME,
                   I.INT_LOG_REQU_IN, I.INT_LOG_REQU_OUT, I.INT_PID,
                   I.INT_PRIM_ADRE, I.INT_PRIM_PORT, I.INT_SECO_ADRE, I.INT_SECO_PORT,
                   I.INT_NUMB_THRE, I.INT_NUMB_PROC, I.INT_FLAG_RECO, I.INT_NUMB_RECO,
                   I.INT_DELA_CONN, I.INT_TIME_OUT_ACQU, I.INT_TIME_OUT_ISSU,
                   I.INT_STAR_SCRI, I.INT_STOP_SCRI, I.INT_TRAC_FILE, I.INT_TRAC_LEVE,
                   I.INT_CURR_FILE_EXT, I.INT_TRAC_REFR_METH, I.INT_TRAC_FILE_PERI,
                   I.INT_TRAC_FILE_SIZE, I.INT_DEST_STAT_ID, I.INT_SOUR_STAT_ID,
                   I.INT_VOIX_DEBI, I.INT_ITY_CODE, I.INT_PING_FREQ, I.INT_SUPE_PING_FLAG,
                   I.INT_AUTO_STAR, I.INT_AUTO_STOP, I.INT_STOP_DECI_FREQ,
                   I.INT_HSM_PROD_TYPE, I.INT_HSM_VERI_TYPE, I.INT_HKY_CODE,
                   I.INT_PIN_LENG, I.INT_BAN_CODE, S.IST_LABE AS STATUS_LABEL
              FROM INTERFACES I
              LEFT JOIN INTERFACE_STATUS S ON S.IST_CODE = I.INT_STAT
             WHERE I.INT_BAN_CODE = 20
             ORDER BY I.INT_CODE
            """;

    public List<Map<String, Object>> findAtmInterfaces() {
        return jdbc.queryForList(SELECT, Map.of());
    }
}
