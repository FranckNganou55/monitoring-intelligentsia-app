package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsTransactionRepository extends JpaRepository<CmsTransaction, Long> {

    /**
     * Returns transactions grouped by network/acquirer/issuer/responseCode for the last {@code intervalMinutes} minutes.
     * @param dateJour       current timestamp
     * @param intervalMinutes window size in minutes (e.g. 15)
     */
    @Query(value = "SELECT ROWNUM id, network, responseCode, acquirer, issuer, responseDesc, nbre"
            + " FROM (SELECT DECODE(BI.BAN_IDEN, BA.BAN_IDEN, 'LOCAL', S.SYS_CORP_IDEN) network,"
            + " A.AUT_RESP_CODE_F039_2 responseCode, NVL(BA.BAN_IDEN, '0000') acquirer,"
            + " NVL(BI.BAN_IDEN, '0000') issuer, R.ARE_LABE responseDesc, COUNT(*) nbre"
            + " FROM ONLINE_AUTHORIZATION A"
            + " INNER JOIN AUTHORIZATION_RESPONSE_F039 R ON A.AUT_RESP_CODE_F039_2 = R.ARE_CODE"
            + " LEFT OUTER JOIN BANK BI ON A.AUT_ISSU_BANK_CODE = BI.BAN_CODE"
            + " LEFT OUTER JOIN SYSTEM S ON A.AUT_SYS_CODE = S.SYS_CODE"
            + " LEFT OUTER JOIN BANK BA ON A.AUT_ACQ_BANK_CODE = BA.BAN_CODE"
            + " WHERE A.AUT_DATE_TIME_TRAN_F012 >= :dateJour - (1 / 1440 * :intervalMinutes)"
            + " GROUP BY DECODE(BI.BAN_IDEN, BA.BAN_IDEN, 'LOCAL', S.SYS_CORP_IDEN),"
            + " A.AUT_RESP_CODE_F039_2, NVL(BI.BAN_IDEN, '0000'), NVL(BA.BAN_IDEN, '0000'), R.ARE_LABE)"
            + " WHERE network IS NOT NULL", nativeQuery = true)
    List<CmsTransaction> findTransaction(@Param("dateJour") Date dateJour,
                                         @Param("intervalMinutes") int intervalMinutes);
}
