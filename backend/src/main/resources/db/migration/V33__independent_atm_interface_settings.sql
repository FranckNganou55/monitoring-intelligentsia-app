-- Preserve existing choices while splitting DOWN and SIGNOFF configuration.
INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible)
SELECT 'alerts.atm.interface.' || status || '.consecutive.samples',
       COALESCE((SELECT valeur FROM app_settings WHERE cle='alerts.atm.interface.consecutive.samples'), '5'),
       'INTEGER', 'ALERTES', 'Relevés consécutifs — ' || status,
       'Nombre de relevés consécutifs de ce statut avant alerte (1 à 20).', TRUE, FALSE
FROM (VALUES ('DOWN'), ('SIGNOFF')) AS statuses(status)
ON CONFLICT (cle) DO NOTHING;
INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible)
SELECT 'alerts.atm.interface.' || status || '.interval.ms',
       COALESCE((SELECT interval_ms::text FROM scheduler_tasks WHERE code='ATM_QUERY'), '180000'),
       'INTEGER', 'ALERTES', 'Fréquence des relevés — ' || status,
       'Intervalle minimum entre deux relevés comptés pour ce statut (60000 à 900000 ms).', TRUE, FALSE
FROM (VALUES ('DOWN'), ('SIGNOFF')) AS statuses(status)
ON CONFLICT (cle) DO NOTHING;
UPDATE app_settings SET modifiable_par_admin=FALSE,
 label='Ancien seuil partagé (remplacé par DOWN et SIGNOFF)',
 description='Conservé pour compatibilité; utiliser les deux réglages indépendants.'
WHERE cle='alerts.atm.interface.consecutive.samples';
