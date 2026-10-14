-- Idempotent test-data sync from types_transactions_suspect.xlsx.
-- One configurable monitoring rule per distinct RISK_RULE_CODE.
-- Risk level is left at the V13 default (MOYEN) for administrator review.
BEGIN;

INSERT INTO monitoring_rules
    (code, name, description, category, enabled, notification_enabled)
VALUES
    ('RULE_01', 'Client uses its maximum limit', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_01
Client uses its maximum limit
Three consecutive POS transactions with incorrect CVV2 in less than 5 minutes
Maximum trial of wrong PIN
Successive declined transactions
Three consecutive ATM transactions with PIN Code Error in less than 5 minutes
GROUPE1 Transaction (DAILY_NBR_TYPE_RESPONSECODE)
Client uses its maximum limits
Three consecutive transactions using expired cards in less than 1 minute
20 TRX decline en 1 Jour
Three POS transactions with insufficient funds error in less than 20 seconds', 'FRAUD', TRUE, TRUE),
    ('RULE_02', 'GROUPE2 Transaction (DAILY_NBR_TYPE) SAME MERCHANT SAME AMOU', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_02
GROUPE2 Transaction (DAILY_NBR_TYPE) SAME MERCHANT SAME AMOU
Multiple transactions with same amount at the same merchant
03 transactions de meme montant chez le meme marchand en 5 minutes', 'FRAUD', TRUE, TRUE),
    ('RULE_03', 'Five internet transactions at the same store within 24 hours', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_03
Five internet transactions at the same store within 24 hours
More than x transactions at one merchant on same day
GROUPE3 Transaction (DAILY_NBR_TYPE) SAME MERCHANT
Five POS transactions at the same store within 24 hours
Plus de 05 transactions chez le meme marchand en 24 heures
Plus de 10 transactions chez le meme marchand en 24 heures', 'FRAUD', TRUE, TRUE),
    ('RULE_04', 'Same days two transaction in different countries', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_04
Same days two transaction in different countries', 'FRAUD', TRUE, TRUE),
    ('RULE_05', 'Any high value transaction', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_05
Any high value transaction', 'FRAUD', TRUE, TRUE),
    ('RULE_06', 'Plus de 5 transactions sur plusieurs marchands de meme categorie en 24h', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_06
Plus de 5 transactions sur plusieurs marchands de meme categorie en 24h
Multiple transactions with same MCC on same day
Plus de 10 transactions sur plusieurs marchands de meme categorie en 3 jours', 'FRAUD', TRUE, TRUE),
    ('RULE_10', 'the consecutive txns of the same type (unsuccessful) in less', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_10
the consecutive txns of the same type (unsuccessful) in less
Three consecutive txns of the same type (first two was unsuccessful) done in less than 30 mins and in decreasing amount', 'FRAUD', TRUE, TRUE),
    ('RULE_11', '3 consecutive trs of the same type in less than 10s', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_11
3 consecutive trs of the same type in less than 10s
2 consecutive trs of the same type in less than 10s', 'FRAUD', TRUE, TRUE),
    ('RULE_12', '03 echecs consecutifs sur marchand avec des montants degressifs en 1h', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_12
03 echecs consecutifs sur marchand avec des montants degressifs en 1h
the POS trs amount of current Trs is considerably prev trs don
3 echecs sur GAB avec des montants dégressifs en 1 heure', 'FRAUD', TRUE, TRUE),
    ('RULE_13', 'POS Cash Advance (US-ON-THEM)', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_13
POS Cash Advance (US-ON-THEM)', 'FRAUD', TRUE, TRUE),
    ('RULE_14', 'FRAUD_One internet transaction without CVV2', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_14
FRAUD_One internet transaction without CVV2', 'FRAUD', TRUE, TRUE),
    ('RULE_15', '5 internet trs without CVV2 in less than 5 mins', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_15
5 internet trs without CVV2 in less than 5 mins', 'FRAUD', TRUE, TRUE),
    ('RULE_17', 'POS trs without PIN with amount greater than 10,000 XFA', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_17
POS trs without PIN with amount greater than 10,000 XFA
POS trs without PIN with amount greater than 60,000 XFA
POS trs without PIN with amount greater than 100,000 XFA', 'FRAUD', TRUE, TRUE),
    ('RULE_19', 'Withdrawals from ATMs located in countries categorized as Hi', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_19
Withdrawals from ATMs located in countries categorized as Hi', 'FRAUD', TRUE, TRUE),
    ('RULE_20', 'Card is entered manually for the transaction even though the', 'Source: types_transactions_suspect.xlsx; RISK_RULE_CODE=RULE_20
Card is entered manually for the transaction even though the', 'FRAUD', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

UPDATE monitoring_alerts a
SET institution_id = i.id,
    archived_at = CASE WHEN a.status = 'ARCHIVED' THEN COALESCE(a.archived_at, a.detected_at) ELSE a.archived_at END,
    updated_at = NOW()
FROM institutions i
JOIN monitoring_rules r ON r.code = 'TEST_FRAUD_ALERT'
WHERE i.code = 'TEST-MONITORING-20260929'
  AND a.rule_id = r.id
  AND a.institution_id IS NULL
  AND a.title ILIKE 'Test %';

INSERT INTO monitoring_alerts
    (rule_id, category, risk_level, title, description, status, institution_id, detected_at, created_at, updated_at)
SELECT r.id, 'FRAUD', r.risk_level,
       '[TEST] RULE_01 - transactions suspectes',
       'TEST - Alerte de démonstration issue du jeu de règles Excel.',
       'OPEN', i.id, NOW(), NOW(), NOW()
FROM monitoring_rules r
JOIN institutions i ON i.code = 'TEST-MONITORING-20260929'
WHERE r.code = 'RULE_01'
  AND NOT EXISTS (
      SELECT 1 FROM monitoring_alerts a
      WHERE a.institution_id = i.id AND a.title = '[TEST] RULE_01 - transactions suspectes'
  );

INSERT INTO monitoring_alerts
    (rule_id, category, risk_level, title, description, status, institution_id, detected_at, created_at, updated_at)
SELECT r.id, 'FRAUD', r.risk_level,
       '[TEST] RULE_06 - activité suspecte',
       'TEST - Alerte active de démonstration.',
       'OPEN', i.id, NOW() - INTERVAL '15 minutes', NOW() - INTERVAL '15 minutes', NOW()
FROM monitoring_rules r
JOIN institutions i ON i.code = 'TEST-MONITORING-20260929'
WHERE r.code = 'RULE_06'
  AND NOT EXISTS (
      SELECT 1 FROM monitoring_alerts a
      WHERE a.institution_id = i.id AND a.title = '[TEST] RULE_06 - activité suspecte'
  );

INSERT INTO alert_events (alert_id, event_type, new_status, comment, details, created_at)
SELECT a.id, 'CREATED', 'OPEN', 'TEST - Création de données de démonstration.',
       '{"source":"types_transactions_suspect.xlsx"}'::jsonb, a.detected_at
FROM monitoring_alerts a
JOIN institutions i ON i.id = a.institution_id
WHERE i.code = 'TEST-MONITORING-20260929'
  AND a.title IN ('[TEST] RULE_01 - transactions suspectes', '[TEST] RULE_06 - activité suspecte')
  AND NOT EXISTS (
      SELECT 1 FROM alert_events e WHERE e.alert_id = a.id AND e.event_type = 'CREATED'
  );

COMMIT;
