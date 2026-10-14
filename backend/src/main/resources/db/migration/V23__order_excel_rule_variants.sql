-- Keep the 39 source rows in workbook order and preserve repeated rule codes.
ALTER TABLE monitoring_rules
    ADD COLUMN source_order INTEGER;

WITH source_rules(source_order, code, name, legacy_name) AS (
    VALUES
        (1, 'RULE_01', 'Client uses its maximum limit', 'Client uses its maximum limit'),
        (2, 'RULE_06', 'Plus de 5 transactions sur plusieurs marchands de meme categorie en 24h', 'Plus de 5 transactions sur plusieurs marchands de même catégorie en 24h'),
        (3, 'RULE_12', '03 echecs consecutifs sur marchand avec des montants degressifs en 1h', '03 échecs consécutifs sur marchand avec des montants dégressifs en 1h'),
        (4, 'RULE_01', 'Three consecutive POS transactions with incorrect CVV2 in less than 5 minutes', 'Three consecutive POS transactions with incorrect CVV2 in less than 5 minutes'),
        (5, 'RULE_01', 'Maximum trial of wrong PIN', 'Maximum trial of wrong PIN'),
        (6, 'RULE_10', 'the consecutive txns of the same type (unsuccessful) in less', 'the consecutive txns of the same type (unsuccessful) in less'),
        (7, 'RULE_03', 'Five internet transactions at the same store within 24 hours', 'Five internet transactions at the same store within 24 hours'),
        (8, 'RULE_01', 'Successive declined transactions', 'Successive declined transactions'),
        (9, 'RULE_01', 'Three consecutive ATM transactions with PIN Code Error in less than 5 minutes', 'Three consecutive ATM transactions with PIN Code Error in less than 5 minutes'),
        (10, 'RULE_11', '3 consecutive trs of the same type in less than 10s', '3 consecutive trs of the same type in less than 10s'),
        (11, 'RULE_02', 'GROUPE2 Transaction (DAILY_NBR_TYPE) SAME MERCHANT SAME AMOU', 'GROUPE2 Transaction (DAILY_NBR_TYPE) SAME MERCHANT SAME AMOU'),
        (12, 'RULE_15', '5 internet trs without CVV2 in less than 5 mins', '5 internet trs without CVV2 in less than 5 mins'),
        (13, 'RULE_20', 'Card is entered manually for the transaction even though the', 'Card is entered manually for the transaction even though the'),
        (14, 'RULE_01', 'GROUPE1 Transaction (DAILY_NBR_TYPE_RESPONSECODE)', 'GROUPE1 Transaction (DAILY_NBR_TYPE_RESPONSECODE)'),
        (15, 'RULE_17', 'POS trs without PIN with amount greater than 10,000 XFA', 'POS trs without PIN with amount greater than 10,000 XFA'),
        (16, 'RULE_01', 'Client uses its maximum limits', 'Client uses its maximum limits'),
        (17, 'RULE_01', 'Three consecutive transactions using expired cards in less than 1 minute', 'Three consecutive transactions using expired cards in less than 1 minute'),
        (18, 'RULE_13', 'POS Cash Advance (US-ON-THEM)', 'POS Cash Advance (US-ON-THEM)'),
        (19, 'RULE_05', 'Any high value transaction', 'Any high value transaction'),
        (20, 'RULE_02', 'Multiple transactions with same amount at the same merchant', 'Multiple transactions with same amount at the same merchant'),
        (21, 'RULE_02', '03 transactions de meme montant chez le meme marchand en 5 minutes', '03 transactions de même montant chez le même marchand en 5 minutes'),
        (22, 'RULE_11', '2 consecutive trs of the same type in less than 10s', '2 consecutive trs of the same type in less than 10s'),
        (23, 'RULE_12', 'the POS trs amount of current Trs is considerably prev trs don', 'the POS trs amount of current Trs is considerably prev trs don'),
        (24, 'RULE_10', 'Three consecutive txns of the same type (first two was unsuccessful) done in less than 30 mins and in decreasing amount', 'Three consecutive txns of the same type (first two was unsuccessful) done in less than 30 mins and in decreasing amount'),
        (25, 'RULE_12', '3 echecs sur GAB avec des montants dégressifs en 1 heure', '3 échecs sur GAB avec des montants dégressifs en 1 heure'),
        (26, 'RULE_17', 'POS trs without PIN with amount greater than 60,000 XFA', 'POS trs without PIN with amount greater than 60,000 XFA'),
        (27, 'RULE_03', 'More than x transactions at one merchant on same day', 'More than x transactions at one merchant on same day'),
        (28, 'RULE_17', 'POS trs without PIN with amount greater than 100,000 XFA', 'POS trs without PIN with amount greater than 100,000 XFA'),
        (29, 'RULE_01', '20 TRX decline en 1 Jour', '20 TRX decline en 1 Jour'),
        (30, 'RULE_06', 'Multiple transactions with same MCC on same day', 'Multiple transactions with same MCC on same day'),
        (31, 'RULE_04', 'Same days two transaction in different countries', 'Same days two transaction in different countries'),
        (32, 'RULE_14', 'FRAUD_One internet transaction without CVV2', 'FRAUD_One internet transaction without CVV2'),
        (33, 'RULE_01', 'Three POS transactions with insufficient funds error in less than 20 seconds', 'Three POS transactions with insufficient funds error in less than 20 seconds'),
        (34, 'RULE_03', 'GROUPE3 Transaction (DAILY_NBR_TYPE) SAME MERCHANT', 'GROUPE3 Transaction (DAILY_NBR_TYPE) SAME MERCHANT'),
        (35, 'RULE_03', 'Five POS transactions at the same store within 24 hours', 'Five POS transactions at the same store within 24 hours'),
        (36, 'RULE_03', 'Plus de 05 transactions chez le meme marchand en 24 heures', 'Plus de 05 transactions chez le même marchand en 24 heures'),
        (37, 'RULE_19', 'Withdrawals from ATMs located in countries categorized as Hi', 'Withdrawals from ATMs located in countries categorized as Hi'),
        (38, 'RULE_06', 'Plus de 10 transactions sur plusieurs marchands de meme categorie en 3 jours', 'Plus de 10 transactions sur plusieurs marchands de même catégorie en 3 jours'),
        (39, 'RULE_03', 'Plus de 10 transactions chez le meme marchand en 24 heures', 'Plus de 10 transactions chez le même marchand en 24 heures')
)
UPDATE monitoring_rules rule
SET name = source.name,
    source_order = source.source_order
FROM source_rules source
WHERE rule.code = source.code
  AND rule.name IN (source.name, source.legacy_name);
