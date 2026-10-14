INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible)
VALUES (
    'alerts.atm.interface.consecutive.samples',
    '5',
    'INTEGER',
    'CRON',
    'Relevés GAB consécutifs avant alerte',
    'Nombre de relevés DOWN (0) ou SIGNOFF (3) consécutifs requis avant de créer une alerte GAB (entre 1 et 20).',
    TRUE,
    FALSE
)
ON CONFLICT (cle) DO NOTHING;

UPDATE monitoring_rules
SET name = 'Interface GAB DOWN ou SIGNOFF',
    description = 'Une alerte est créée après le nombre configuré de relevés consécutifs DOWN (0) ou SIGNOFF (3). L’intervalle de collecte ATM_QUERY est paramétrable dans Administration > Paramètres > Crons > Alertes.'
WHERE code = 'RULE_ATM_INTERFACE_UNAVAILABLE_5X';
