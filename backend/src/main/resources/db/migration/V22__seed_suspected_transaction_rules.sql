-- Import the distinct SVFE rule descriptions supplied in types_transactions_suspect.xlsx.
-- Repeated rule codes are intentionally kept as separate rows because each description
-- identifies a distinct rule variant in the source workbook.
WITH source_rules(code, name) AS (
    VALUES
        ('RULE_01', 'Client uses its maximum limit'),
        ('RULE_06', 'Plus de 5 transactions sur plusieurs marchands de même catégorie en 24h'),
        ('RULE_12', '03 échecs consécutifs sur marchand avec des montants dégressifs en 1h'),
        ('RULE_01', 'Three consecutive POS transactions with incorrect CVV2 in less than 5 minutes'),
        ('RULE_01', 'Maximum trial of wrong PIN'),
        ('RULE_10', 'the consecutive txns of the same type (unsuccessful) in less'),
        ('RULE_03', 'Five internet transactions at the same store within 24 hours'),
        ('RULE_01', 'Successive declined transactions'),
        ('RULE_01', 'Three consecutive ATM transactions with PIN Code Error in less than 5 minutes'),
        ('RULE_11', '3 consecutive trs of the same type in less than 10s'),
        ('RULE_02', 'GROUPE2 Transaction (DAILY_NBR_TYPE) SAME MERCHANT SAME AMOU'),
        ('RULE_15', '5 internet trs without CVV2 in less than 5 mins'),
        ('RULE_20', 'Card is entered manually for the transaction even though the'),
        ('RULE_01', 'GROUPE1 Transaction (DAILY_NBR_TYPE_RESPONSECODE)'),
        ('RULE_17', 'POS trs without PIN with amount greater than 10,000 XFA'),
        ('RULE_01', 'Client uses its maximum limits'),
        ('RULE_01', 'Three consecutive transactions using expired cards in less than 1 minute'),
        ('RULE_13', 'POS Cash Advance (US-ON-THEM)'),
        ('RULE_05', 'Any high value transaction'),
        ('RULE_02', 'Multiple transactions with same amount at the same merchant'),
        ('RULE_02', '03 transactions de même montant chez le même marchand en 5 minutes'),
        ('RULE_11', '2 consecutive trs of the same type in less than 10s'),
        ('RULE_12', 'the POS trs amount of current Trs is considerably prev trs don'),
        ('RULE_10', 'Three consecutive txns of the same type (first two was unsuccessful) done in less than 30 mins and in decreasing amount'),
        ('RULE_12', '3 échecs sur GAB avec des montants dégressifs en 1 heure'),
        ('RULE_17', 'POS trs without PIN with amount greater than 60,000 XFA'),
        ('RULE_03', 'More than x transactions at one merchant on same day'),
        ('RULE_17', 'POS trs without PIN with amount greater than 100,000 XFA'),
        ('RULE_01', '20 TRX decline en 1 Jour'),
        ('RULE_06', 'Multiple transactions with same MCC on same day'),
        ('RULE_04', 'Same days two transaction in different countries'),
        ('RULE_14', 'FRAUD_One internet transaction without CVV2'),
        ('RULE_01', 'Three POS transactions with insufficient funds error in less than 20 seconds'),
        ('RULE_03', 'GROUPE3 Transaction (DAILY_NBR_TYPE) SAME MERCHANT'),
        ('RULE_03', 'Five POS transactions at the same store within 24 hours'),
        ('RULE_03', 'Plus de 05 transactions chez le même marchand en 24 heures'),
        ('RULE_19', 'Withdrawals from ATMs located in countries categorized as Hi'),
        ('RULE_06', 'Plus de 10 transactions sur plusieurs marchands de même catégorie en 3 jours'),
        ('RULE_03', 'Plus de 10 transactions chez le même marchand en 24 heures')
)
INSERT INTO monitoring_rules (
    code,
    name,
    description,
    category,
    risk_level,
    enabled,
    notification_enabled,
    sound_enabled,
    visual_alert_enabled,
    risk_percentage
)
SELECT source_rules.code,
       source_rules.name,
       source_rules.name,
       'FRAUD',
       'MOYEN',
       TRUE,
       TRUE,
       FALSE,
       FALSE,
       0.00
FROM source_rules
WHERE NOT EXISTS (
    SELECT 1
    FROM monitoring_rules existing
    WHERE existing.code = source_rules.code
      AND existing.name = source_rules.name
);
