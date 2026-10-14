package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsAccount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsAccountRepository extends JpaRepository<CmsAccount, Long> {

    @Query(value = "SELECT ROWNUM id, institution, currency, balance, '' account"
            + " FROM (SELECT BAN_IDEN institution, CUR_IDE currency, SUM(Solde) balance"
            + " FROM (SELECT b.BAN_IDEN, CUR_IDE,"
            + " NVL(acc_avai_amou,0) - NVL(acc_auth_cumu,0) - NVL(acc_used_cumu,0) AS Solde"
            + " FROM card INNER JOIN account ON card.CAR_BAN_CODE = account.ACC_BAN_CODE"
            + " INNER JOIN cardholder_routing ON account.acc_code = cardholder_routing.cro_acc_code"
            + " INNER JOIN bank b ON card.CAR_BAN_CODE = b.ban_code"
            + " INNER JOIN currency ON account.acc_curr_code = currency.cur_code"
            + " WHERE car_cty_code = 4 AND car_numb = cro_pan AND cro_acc_code = acc_code"
            + " AND NVL(acc_avai_amou,0) - NVL(acc_auth_cumu,0) - NVL(acc_used_cumu,0) <> 0)"
            + " GROUP BY BAN_IDEN, CUR_IDE)", nativeQuery = true)
    List<CmsAccount> findSoldeFloat();
}
