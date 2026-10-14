-- Keep the alert cadence controls usable even when scheduler rows were missing in an older database.
-- The settings table originally constrained the group list to the V10 values.
ALTER TABLE app_settings DROP CONSTRAINT IF EXISTS app_settings_groupe_check;
ALTER TABLE app_settings
    ADD CONSTRAINT app_settings_groupe_check
    CHECK (groupe IN ('MAIL', 'CRON', 'ALERTES', 'SECURITE', 'GENERAL', 'NOTIFICATIONS'));

INSERT INTO scheduler_tasks (code, name, enabled, interval_ms)
VALUES
    ('ATM_QUERY', 'Requête ATM', TRUE, 180000),
    ('FRAUD_QUERY', 'Requête Fraude', TRUE, 60000)
ON CONFLICT (code) DO NOTHING;

UPDATE app_settings
SET groupe = 'ALERTES',
    description = 'Nombre de relevés DOWN (0) ou SIGNOFF (3) consécutifs requis avant alerte (entre 1 et 20).'
WHERE cle = 'alerts.atm.interface.consecutive.samples';

UPDATE monitoring_rules
SET description = 'Une alerte est créée après le nombre configuré de relevés consécutifs DOWN (0) ou SIGNOFF (3). L’intervalle des relevés est paramétrable dans Administration > Paramètres > Alertes.'
WHERE code = 'RULE_ATM_INTERFACE_UNAVAILABLE_5X';

-- Keep read notifications for one day after they are read, then allow cleanup.
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;
UPDATE notifications SET read_at = created_at AT TIME ZONE 'UTC'
WHERE read = TRUE AND read_at IS NULL;
