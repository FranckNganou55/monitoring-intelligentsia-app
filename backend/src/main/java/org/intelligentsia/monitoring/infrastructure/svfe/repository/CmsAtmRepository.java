package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.SSAtm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Queries the SSAtm Oracle table (one row per cassette slot).
 * CmsAtm objects are built in-memory by grouping SSAtm rows by terminalId.
 */
@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsAtmRepository extends JpaRepository<SSAtm, Integer> {

    @Query(value = "SELECT ROWNUM id, T.ATE_CODE pid, B.BAN_IDEN institution, T.ATE_NUM terminalId, C.ATC_CASS cassette,"
            + " NVL(C.ATC_REMA_NOTE, 0) nombreRest, NVL(C.ATC_REJE_NOTE, 0) nombreRej, NVL(C.ATC_DISP_NOTE, 0) nombreDep,"
            + " T.ATE_LABE adress, V.CIT_IDEN ville, CU.CUR_IDE currency, D.ACD_VALU note,"
            + " T.ATE_AMO_CODE statusId, M.AMO_LABE statusDesc"
            + " FROM ATM_TERMINAL T"
            + " INNER JOIN BANK B ON T.ATE_BAN_CODE = B.BAN_CODE"
            + " INNER JOIN ATM_TERMINAL_CASSETTE_COUNT C ON T.ATE_CODE = C.ATC_ATE_CODE"
            + " INNER JOIN CITY V ON T.ATE_CIT_CODE = V.CIT_CODE"
            + " INNER JOIN ATM_MODE M ON T.ATE_AMO_CODE = M.AMO_CODE"
            + " INNER JOIN ATM_CONFIGURATION O ON T.ATE_ACO_CODE = O.ACO_CODE"
            + " INNER JOIN ATM_DENOMINATION_CONFIG DO ON O.ACO_ADC_CODE = DO.ADC_CODE"
            + " INNER JOIN ATM_CASSETTE_DENOMINATION D ON DO.ADC_CODE = D.ACD_ADC_CODE AND D.ACD_CASS = C.ATC_CASS"
            + " INNER JOIN CURRENCY CU ON D.ACD_CUR_CODE = CU.CUR_CODE"
            + " WHERE ATE_AGR_CODE != 1005700"
            + " ORDER BY T.ATE_NUM, C.ATC_CASS", nativeQuery = true)
    List<SSAtm> findATMs();
}
