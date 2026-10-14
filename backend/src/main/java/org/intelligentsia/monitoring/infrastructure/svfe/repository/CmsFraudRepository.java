package org.intelligentsia.monitoring.infrastructure.svfe.repository;

import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsFraud;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
public interface CmsFraudRepository extends JpaRepository<CmsFraud, Long> {

    @Query(value = "SELECT ROWNUM id, acquirer, issuer, bin, status, rules, description, Risk_Level, risk_auth_code, retrieval_reference"
            + " FROM (SELECT NVL((SELECT BAN_IDEN FROM BANK WHERE BAN_CODE = C.AUT_ACQ_BANK_CODE), '0000') acquirer,"
            + " NVL((SELECT BAN_IDEN FROM BANK WHERE BAN_CODE = C.AUT_ISSU_BANK_CODE), '0000') issuer,"
            + " SUBSTR(SA.PRIM_ACCT_NUMB_F002, 0, 6) BIN, 1 STATUS, SAD.RISK_RULE_CODE RULES,"
            + " RULE_DESCRIPTION description, SAD.RISK_LEVEL Risk_Level, SAD.RISK_AUTH_CODE risk_auth_code,"
            + " SA.RETR_REF_NUMB_F037 retrieval_reference"
            + " FROM SUSPECTED_AUTHORIZATION SA"
            + " INNER JOIN ONLINE_AUTHORIZATION C ON C.AUT_RETR_REF_NUMB_F037 = SA.RETR_REF_NUMB_F037"
            + " INNER JOIN SUSPECTED_AUTHORIZATION_DETAIL SAD ON SAD.RISK_AUTH_CODE = SA.RISK_AUTH_CODE"
            + " ORDER BY bin)", nativeQuery = true)
    List<CmsFraud> findFraud();
}
