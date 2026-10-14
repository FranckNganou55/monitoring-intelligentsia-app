-- V1 : Tables d'authentification et de sécurité

CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id       BIGSERIAL    PRIMARY KEY,
    code     VARCHAR(100) NOT NULL UNIQUE,
    name     VARCHAR(100) NOT NULL,
    resource VARCHAR(50)  NOT NULL,
    action   VARCHAR(20)  NOT NULL
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id                  BIGSERIAL    PRIMARY KEY,
    login               VARCHAR(100) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(100) NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    date_birth          DATE,
    phone               VARCHAR(20),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    language_code       VARCHAR(5)   NOT NULL DEFAULT 'fr',
    institution_id      BIGINT,
    password_expires_at TIMESTAMP,
    account_expires_at  TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE password_history (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_institution  ON users(institution_id);
CREATE INDEX idx_users_status       ON users(status);
CREATE INDEX idx_refresh_tokens_uid ON refresh_tokens(user_id);

-- Rôles par défaut
INSERT INTO roles (code, name, description) VALUES
    ('ROLE_ADMIN', 'Administrateur', 'Accès complet à toutes les fonctionnalités'),
    ('ROLE_MANAGER', 'Responsable', 'Accès aux données de toutes les institutions, lecture seule config'),
    ('ROLE_OPERATOR', 'Opérateur', 'Accès aux données de son institution uniquement');

-- Permissions
INSERT INTO permissions (code, name, resource, action) VALUES
    ('USER_READ',        'Consulter les utilisateurs',      'USER',        'READ'),
    ('USER_WRITE',       'Gérer les utilisateurs',          'USER',        'WRITE'),
    ('USER_DELETE',      'Supprimer les utilisateurs',      'USER',        'DELETE'),
    ('INSTITUTION_READ', 'Consulter les institutions',      'INSTITUTION', 'READ'),
    ('INSTITUTION_WRITE','Gérer les institutions',          'INSTITUTION', 'WRITE'),
    ('ATM_READ',         'Consulter les ATMs',              'ATM',         'READ'),
    ('ATM_WRITE',        'Gérer les ATMs',                  'ATM',         'WRITE'),
    ('POS_READ',         'Consulter les POS',               'POS',         'READ'),
    ('POS_WRITE',        'Gérer les POS',                   'POS',         'WRITE'),
    ('DASHBOARD_READ',   'Consulter le tableau de bord',    'DASHBOARD',   'READ'),
    ('FRAUD_READ',       'Consulter la fraude',             'FRAUD',       'READ'),
    ('FLEET_READ',       'Consulter la flotte',             'FLEET',       'READ'),
    ('EXTRACTION_READ',  'Consulter les extractions',       'EXTRACTION',  'READ'),
    ('EXTRACTION_WRITE', 'Gérer les extractions',           'EXTRACTION',  'WRITE'),
    ('AUDIT_READ',       'Consulter les journaux d''audit', 'AUDIT',       'READ'),
    ('SCHEDULER_WRITE',  'Gérer les tâches planifiées',     'SCHEDULER',   'WRITE');

-- Affecter toutes les permissions à ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.code = 'ROLE_ADMIN';

-- Permissions MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ROLE_MANAGER'
  AND p.code IN ('DASHBOARD_READ','ATM_READ','POS_READ','FRAUD_READ','FLEET_READ',
                 'EXTRACTION_READ','EXTRACTION_WRITE','INSTITUTION_READ','AUDIT_READ');

-- Permissions OPERATOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ROLE_OPERATOR'
  AND p.code IN ('DASHBOARD_READ','ATM_READ','POS_READ','FRAUD_READ','FLEET_READ',
                 'EXTRACTION_READ','EXTRACTION_WRITE');
