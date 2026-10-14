-- V17__extend_notifications.sql

ALTER TABLE notifications
ADD COLUMN alert_id BIGINT
    REFERENCES monitoring_alerts(id)
    ON DELETE SET NULL;

ALTER TABLE notifications
ADD COLUMN risk_level VARCHAR(30)
    CHECK (risk_level IN (
        'TYPIQUE',
        'MOINS_CRITIQUE',
        'MOYEN',
        'CRITIQUE'
    ));

ALTER TABLE notifications
ADD COLUMN sound_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE notifications
ADD COLUMN visual_alert_required BOOLEAN NOT NULL DEFAULT FALSE;


CREATE INDEX idx_notifications_alert
    ON notifications(alert_id);

CREATE INDEX idx_notifications_risk_level
    ON notifications(risk_level);

CREATE INDEX idx_notifications_user_read
    ON notifications(user_id, read);


INSERT INTO app_settings
(
    cle,
    valeur,
    type,
    groupe,
    label,
    description,
    modifiable_par_admin,
    sensible
)
VALUES
(
    'notif.sound.enabled',
    'true',
    'BOOLEAN',
    'NOTIFICATIONS',
    'Notifications sonores',
    'Active les notifications sonores',
    TRUE,
    FALSE
),
(
    'notif.sound.volume',
    '80',
    'INTEGER',
    'NOTIFICATIONS',
    'Volume des notifications',
    'Volume sonore entre 0 et 100',
    TRUE,
    FALSE
),
(
    'notif.sound.medium.enabled',
    'true',
    'BOOLEAN',
    'NOTIFICATIONS',
    'Son niveau moyen',
    'Active le son pour les alertes MOYEN',
    TRUE,
    FALSE
),
(
    'notif.sound.critical.enabled',
    'true',
    'BOOLEAN',
    'NOTIFICATIONS',
    'Son niveau critique',
    'Active le son pour les alertes CRITIQUE',
    TRUE,
    FALSE
),
(
    'notif.visual.medium.enabled',
    'true',
    'BOOLEAN',
    'NOTIFICATIONS',
    'Alerte visuelle niveau moyen',
    'Active le signal visuel pour les alertes MOYEN',
    TRUE,
    FALSE
),
(
    'notif.visual.critical.enabled',
    'true',
    'BOOLEAN',
    'NOTIFICATIONS',
    'Alerte visuelle niveau critique',
    'Active le signal visuel pour les alertes CRITIQUE',
    TRUE,
    FALSE
);