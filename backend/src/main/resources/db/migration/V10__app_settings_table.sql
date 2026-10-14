-- V10 : table des paramètres de l'application

CREATE TABLE app_settings (
    id                  BIGSERIAL    PRIMARY KEY,
    cle                 VARCHAR(100) NOT NULL UNIQUE,
    valeur              TEXT,
    type                VARCHAR(20)  NOT NULL CHECK (type IN ('STRING','INTEGER','BOOLEAN','EMAIL','PASSWORD','JSON')),
    groupe              VARCHAR(20)  NOT NULL CHECK (groupe IN ('MAIL','CRON','SECURITE','GENERAL','NOTIFICATIONS')),
    label               VARCHAR(200) NOT NULL,
    description         TEXT,
    modifiable_par_admin BOOLEAN     NOT NULL DEFAULT TRUE,
    sensible            BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100)
);

CREATE INDEX idx_app_settings_groupe ON app_settings(groupe);

-- ── MAIL ──────────────────────────────────────────────────────────────────────
INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible) VALUES
('mail.smtp.host',     'localhost',              'STRING',  'MAIL', 'Serveur SMTP',           'Adresse du serveur SMTP',                          true, false),
('mail.smtp.port',     '587',                    'INTEGER', 'MAIL', 'Port SMTP',              'Port SMTP (25 = non chiffré, 465 = SSL, 587 = TLS)', true, false),
('mail.smtp.username', '',                       'STRING',  'MAIL', 'Identifiant SMTP',       'Nom d''utilisateur pour l''authentification SMTP', true, false),
('mail.smtp.password', '',                       'PASSWORD','MAIL', 'Mot de passe SMTP',      'Mot de passe SMTP (chiffré en base)',               true, true),
('mail.smtp.ssl',      'false',                  'BOOLEAN', 'MAIL', 'SSL/TLS',                'Activer le chiffrement SSL/TLS sur la connexion',   true, false),
('mail.from.address',  'noreply@monitoring.local','EMAIL',  'MAIL', 'Adresse expéditeur',     'Adresse email affichée dans le champ «De»',         true, false),
('mail.from.name',     'Monitoring Monétique',   'STRING',  'MAIL', 'Nom expéditeur',         'Nom affiché dans le champ «De»',                   true, false),
('mail.enabled',       'false',                  'BOOLEAN', 'MAIL', 'Emails activés',         'Activer ou désactiver l''envoi d''emails',          true, false);

-- ── CRON ──────────────────────────────────────────────────────────────────────
INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible) VALUES
('cron.enabled',             'true', 'BOOLEAN', 'CRON', 'Crons activés',              'Désactiver pour suspendre toutes les tâches planifiées', true, false),
('cron.max.concurrent.jobs', '3',    'INTEGER', 'CRON', 'Jobs simultanés max',        'Nombre maximum d''extractions pouvant s''exécuter en parallèle', true, false),
('cron.retry.attempts',      '2',    'INTEGER', 'CRON', 'Tentatives en cas d''échec', 'Nombre de relances automatiques après un échec',         true, false),
('cron.retry.delay.seconds', '60',   'INTEGER', 'CRON', 'Délai entre tentatives (s)', 'Délai en secondes entre deux relances',                  true, false);

-- ── SECURITE ──────────────────────────────────────────────────────────────────
INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible) VALUES
('security.jwt.expiration.minutes',   '15', 'INTEGER', 'SECURITE', 'Durée token JWT (min)',          'Validité du token d''accès JWT',                         true, false),
('security.refresh.token.days',       '7',  'INTEGER', 'SECURITE', 'Durée refresh token (jours)',    'Validité du token de rafraîchissement',                  true, false),
('security.max.login.attempts',       '5',  'INTEGER', 'SECURITE', 'Tentatives de connexion max',    'Nombre d''échecs consécutifs avant blocage du compte',   true, false),
('security.lockout.duration.minutes', '30', 'INTEGER', 'SECURITE', 'Durée de blocage (min)',         'Durée de blocage du compte après trop de tentatives',    true, false),
('security.password.min.length',      '8',  'INTEGER', 'SECURITE', 'Longueur min. mot de passe',     'Nombre minimum de caractères exigé',                     true, false);

-- ── NOTIFICATIONS ─────────────────────────────────────────────────────────────
INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible) VALUES
('notif.email.enabled',  'true', 'BOOLEAN', 'NOTIFICATIONS', 'Notifications email',        'Envoyer des alertes par email',                            true, false),
('notif.inapp.enabled',  'true', 'BOOLEAN', 'NOTIFICATIONS', 'Notifications in-app',       'Afficher les notifications dans l''interface',             true, false),
('notif.retention.days', '90',   'INTEGER', 'NOTIFICATIONS', 'Rétention notifications (j)', 'Durée de conservation des notifications en base de données', true, false);

-- ── GENERAL ───────────────────────────────────────────────────────────────────
INSERT INTO app_settings (cle, valeur, type, groupe, label, description, modifiable_par_admin, sensible) VALUES
('app.name',             'Monitoring Monétique', 'STRING', 'GENERAL', 'Nom de la plateforme',  'Nom affiché dans le titre de l''application',  true, false),
('app.logo.url',         '',                     'STRING', 'GENERAL', 'URL du logo',           'Chemin ou URL externe du logo',                true, false),
('app.timezone',         'Africa/Dakar',         'STRING', 'GENERAL', 'Fuseau horaire',        'Fuseau horaire par défaut (ex: Africa/Dakar)',  true, false),
('app.dateformat',       'dd/MM/yyyy HH:mm',     'STRING', 'GENERAL', 'Format de date',        'Format d''affichage des dates (Java DateTimeFormatter)', true, false),
('app.language.default', 'fr',                   'STRING', 'GENERAL', 'Langue par défaut',     'Code ISO de la langue (fr, en, ar…)',          true, false);
