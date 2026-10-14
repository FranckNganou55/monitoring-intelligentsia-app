-- V19__monitoring_permissions.sql

-- ============================================================
-- Permissions Monitoring / Fraude / Rapports
-- ============================================================

INSERT INTO permissions (
    code,
    name,
    resource,
    action
)
VALUES
    (
        'MONITORING_RULE_VIEW',
        'Consulter les règles de monitoring',
        'MONITORING_RULE',
        'VIEW'
    ),
    (
        'MONITORING_RULE_CREATE',
        'Créer une règle de monitoring',
        'MONITORING_RULE',
        'CREATE'
    ),
    (
        'MONITORING_RULE_UPDATE',
        'Modifier une règle de monitoring',
        'MONITORING_RULE',
        'UPDATE'
    ),
    (
        'MONITORING_RULE_DELETE',
        'Supprimer une règle de monitoring',
        'MONITORING_RULE',
        'DELETE'
    ),

    (
        'MONITORING_ALERT_VIEW',
        'Consulter les alertes',
        'MONITORING_ALERT',
        'VIEW'
    ),
    (
        'MONITORING_ALERT_ACKNOWLEDGE',
        'Accuser réception d''une alerte',
        'MONITORING_ALERT',
        'ACKNOWLEDGE'
    ),
    (
        'MONITORING_ALERT_ASSIGN',
        'Affecter une alerte',
        'MONITORING_ALERT',
        'ASSIGN'
    ),
    (
        'MONITORING_ALERT_PROCESS',
        'Traiter une alerte',
        'MONITORING_ALERT',
        'PROCESS'
    ),
    (
        'MONITORING_ALERT_RESOLVE',
        'Résoudre une alerte',
        'MONITORING_ALERT',
        'RESOLVE'
    ),
    (
        'MONITORING_ALERT_ARCHIVE',
        'Archiver une alerte',
        'MONITORING_ALERT',
        'ARCHIVE'
    ),

    (
        'FRAUD_DETECTION_VIEW',
        'Consulter les détections de fraude',
        'FRAUD_DETECTION',
        'VIEW'
    ),
    (
        'FRAUD_DETECTION_PROCESS',
        'Traiter une détection de fraude',
        'FRAUD_DETECTION',
        'PROCESS'
    ),

    (
        'FRAUD_REPORT_VIEW',
        'Consulter les rapports de fraude',
        'FRAUD_REPORT',
        'VIEW'
    ),
    (
        'FRAUD_REPORT_GENERATE',
        'Générer un rapport de fraude',
        'FRAUD_REPORT',
        'GENERATE'
    ),
    (
        'FRAUD_REPORT_DOWNLOAD',
        'Télécharger un rapport de fraude',
        'FRAUD_REPORT',
        'DOWNLOAD'
    ),

    (
        'MONITORING_SETTINGS_VIEW',
        'Consulter les paramètres de monitoring',
        'MONITORING_SETTINGS',
        'VIEW'
    ),
    (
        'MONITORING_SETTINGS_UPDATE',
        'Modifier les paramètres de monitoring',
        'MONITORING_SETTINGS',
        'UPDATE'
    )

ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- Attribution des permissions au rôle ADMIN
-- ============================================================

INSERT INTO role_permissions (
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM roles r
JOIN permissions p
    ON p.code IN (
        'MONITORING_RULE_VIEW',
        'MONITORING_RULE_CREATE',
        'MONITORING_RULE_UPDATE',
        'MONITORING_RULE_DELETE',

        'MONITORING_ALERT_VIEW',
        'MONITORING_ALERT_ACKNOWLEDGE',
        'MONITORING_ALERT_ASSIGN',
        'MONITORING_ALERT_PROCESS',
        'MONITORING_ALERT_RESOLVE',
        'MONITORING_ALERT_ARCHIVE',

        'FRAUD_DETECTION_VIEW',
        'FRAUD_DETECTION_PROCESS',

        'FRAUD_REPORT_VIEW',
        'FRAUD_REPORT_GENERATE',
        'FRAUD_REPORT_DOWNLOAD',

        'MONITORING_SETTINGS_VIEW',
        'MONITORING_SETTINGS_UPDATE'
    )
WHERE r.name = 'ROLE_ADMIN'

ON CONFLICT DO NOTHING;