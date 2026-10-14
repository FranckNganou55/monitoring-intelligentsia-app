package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsCardLimit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsCardLimitRepository extends JpaRepository<CmsCardLimit, Long> {

    /**
     * Returns cards for a given institution + currency whose balance exceeds the PP limit.
     * @param institution  SVFE institution code
     * @param currency     ISO numeric currency code (e.g. "952")
     * @param limitAmount  card PP limit threshold
     */
    @Query(value = "SELECT ROWNUM id, ban_iden institution, car_numb cardNumber, car_chld_name embossName,"
            + " cur_ide currency, acc_numb account, solde balance"
            + " FROM (SELECT ban_iden, cur_ide, car_numb, acc_numb, car_chld_name,"
            + " NVL(acc_avai_amou,0) - NVL(acc_auth_cumu,0) - NVL(acc_used_cumu,0) AS solde"
            + " FROM card INNER JOIN account ON card.car_ban_code = account.acc_ban_code"
            + " INNER JOIN bank ON card.car_ban_code = bank.ban_code"
            + " INNER JOIN currency ON account.acc_curr_code = currency.cur_code"
            + " INNER JOIN cardholder_routing ON account.acc_code = cardholder_routing.cro_acc_code"
            + " WHERE car_cty_code = 4 AND car_numb = cro_pan AND cro_acc_code = acc_code"
            + " AND NVL(acc_avai_amou,0) - NVL(acc_auth_cumu,0) - NVL(acc_used_cumu,0) > :maximum"
            + " AND cur_ide = :devise AND ban_iden = :institution)", nativeQuery = true)
    List<CmsCardLimit> findCardLimit(
            @Param("institution") String institution,
            @Param("devise") String devise,
            @Param("maximum") double maximum);

    /** Resolve the embossing/cardholder name for a PAN without exposing the PAN in logs. */
    @Query(value = "SELECT MAX(TRIM(car_chld_name)) FROM card "
            + "WHERE REGEXP_REPLACE(TRIM(car_numb), '[^[:digit:]]', '') = :cardNumber",
            nativeQuery = true)
    String findCardholderNameByCardNumber(@Param("cardNumber") String cardNumber);
}
