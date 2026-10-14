-- V2 : Tables de configuration du monitoring

CREATE TABLE institutions (
    id            BIGSERIAL    PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    svfe_id       VARCHAR(50),
    host_pid      VARCHAR(50),
    mail_recharge VARCHAR(255),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by    BIGINT,
    updated_by    BIGINT
);

ALTER TABLE users ADD CONSTRAINT fk_users_institution
    FOREIGN KEY (institution_id) REFERENCES institutions(id) ON DELETE SET NULL;

CREATE TABLE currencies (
    id          BIGSERIAL    PRIMARY KEY,
    iso_numeric VARCHAR(3)   NOT NULL UNIQUE,
    iso_alpha   VARCHAR(3)   NOT NULL UNIQUE,
    label       VARCHAR(100) NOT NULL
);

CREATE TABLE institution_currencies (
    institution_id BIGINT         NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    currency_id    BIGINT         NOT NULL REFERENCES currencies(id),
    atm_threshold  NUMERIC(20, 2),
    card_pp_limit  NUMERIC(20, 2),
    PRIMARY KEY (institution_id, currency_id)
);

CREATE TABLE atms (
    id             BIGSERIAL    PRIMARY KEY,
    institution_id BIGINT       NOT NULL REFERENCES institutions(id),
    terminal_id    VARCHAR(50)  NOT NULL,
    address        TEXT,
    city           VARCHAR(100),
    currency1_id   BIGINT       REFERENCES currencies(id),
    threshold1     NUMERIC(20, 2),
    currency2_id   BIGINT       REFERENCES currencies(id),
    threshold2     NUMERIC(20, 2),
    svfe_pid       BIGINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT,
    UNIQUE (institution_id, terminal_id)
);

CREATE TABLE technicians (
    id             BIGSERIAL    PRIMARY KEY,
    institution_id BIGINT       NOT NULL REFERENCES institutions(id),
    name           VARCHAR(200) NOT NULL,
    phone          VARCHAR(20),
    cards          TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT
);

CREATE TABLE pos_terminals (
    id             BIGSERIAL   PRIMARY KEY,
    institution_id BIGINT      NOT NULL REFERENCES institutions(id),
    terminal_id    VARCHAR(50) NOT NULL UNIQUE,
    merchant       VARCHAR(200),
    city           VARCHAR(100),
    district       VARCHAR(100),
    extra_name     VARCHAR(200),
    technician_id  BIGINT      REFERENCES technicians(id) ON DELETE SET NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT
);

CREATE TABLE scheduler_tasks (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    interval_ms BIGINT       NOT NULL,
    last_run    TIMESTAMP,
    last_status VARCHAR(20),
    last_error  TEXT
);

CREATE INDEX idx_atms_institution ON atms(institution_id);
CREATE INDEX idx_pos_institution  ON pos_terminals(institution_id);
CREATE INDEX idx_tech_institution ON technicians(institution_id);

-- Données de référence — devises
INSERT INTO currencies (iso_numeric, iso_alpha, label) VALUES
    ('952', 'XOF', 'Franc CFA BCEAO'),
    ('950', 'XAF', 'Franc CFA BEAC'),
    ('840', 'USD', 'Dollar américain'),
    ('978', 'EUR', 'Euro');

-- Tâches de monitoring par défaut
INSERT INTO scheduler_tasks (code, name, enabled, interval_ms) VALUES
    ('ATM_QUERY',        'Requête ATM',                     true,  600000),
    ('NETWORK_QUERY',    'Requête Réseaux',                 true,  300000),
    ('HOST_QUERY',       'Requête Hôtes',                   true,  300000),
    ('TRANSACTION_QUERY','Requête Transactions',            true,  300000),
    ('FRAUD_QUERY',      'Requête Fraude',                  true,  600000),
    ('FLEET_QUERY',      'Requête Flotte',                  true,  600000),
    ('CARDLIMIT_QUERY',  'Requête Cartes Hors Limites',     true,  600000),
    ('TELECOLLECT_QUERY','Requête Télécollecte',            true, 86400000),
    ('RETENTION',        'Purge des données anciennes',     true, 86400000);
