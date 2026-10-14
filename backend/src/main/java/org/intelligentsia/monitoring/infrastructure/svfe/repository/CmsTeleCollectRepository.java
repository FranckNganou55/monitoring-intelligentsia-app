package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsTeleCollect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsTeleCollectRepository extends JpaRepository<CmsTeleCollect, String> {

    /** Returns all POS terminals currently pending telecollect (not yet collected). */
    @Query(value = "SELECT ban_iden institution, mer_corp_name merchant, rem_term_numb terminalId,"
            + " (SELECT TO_CHAR(MIN(rtr_heur_tran),'YYYY/MM/DD HH24:MI:SS') FROM remittance_transactions"
            + "  WHERE rtr_rem_code = rem_code AND NVL(rtr_reve_stat,'A') <> 'V') firstTransDate,"
            + " (SELECT TO_CHAR(MAX(rtr_heur_tran),'YYYY/MM/DD HH24:MI:SS') FROM remittance_transactions"
            + "  WHERE rtr_rem_code = rem_code AND NVL(rtr_reve_stat,'A') <> 'V') lastTransDate,"
            + " rem_debi_tran_numb nombre, rem_tota_debi_amou montant"
            + " FROM remittance rem1, merchant, merchant_activity, bank"
            + " WHERE (rem_stat IS NULL OR (rem_stat = 'N' AND rem_code ="
            + "  (SELECT MAX(rem2.rem_code) FROM remittance rem2 WHERE rem1.rem_term_numb = rem2.rem_term_numb)))"
            + " AND rem_merc_iden = mer_iden"
            + " AND mer_acti_code = mac_code"
            + " AND rem_ban_code = ban_code"
            + " AND EXISTS (SELECT 1 FROM remittance_transactions"
            + "  WHERE rtr_rem_code = rem_code AND NVL(rtr_reve_stat,'A') <> 'V')", nativeQuery = true)
    List<CmsTeleCollect> teleCollectPos();
}
