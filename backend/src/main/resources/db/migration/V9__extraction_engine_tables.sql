-- V9 : Moteur d'extractions configurables

CREATE TABLE extraction_templates (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('SQL_QUERY', 'NAMED_QUERY', 'JASPER_REPORT')),
    content     TEXT         NOT NULL,
    parameters  JSONB,                    -- [{name, type, label, required, defaultValue}]
    module      VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE extraction_configs (
    id              BIGSERIAL    PRIMARY KEY,
    institution_id  BIGINT       NOT NULL REFERENCES institutions(id),
    template_id     BIGINT       NOT NULL REFERENCES extraction_templates(id),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    param_values    JSONB,                -- {"paramName": "value", ...}
    output_format   VARCHAR(10)  NOT NULL CHECK (output_format IN ('CSV', 'EXCEL', 'PDF')),
    cron_expression VARCHAR(100),         -- null = manuel uniquement
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT
);

CREATE TABLE extraction_jobs (
    id            BIGSERIAL    PRIMARY KEY,
    config_id     BIGINT       NOT NULL REFERENCES extraction_configs(id),
    trigger_type  VARCHAR(10)  NOT NULL CHECK (trigger_type IN ('MANUEL', 'CRON')),
    status        VARCHAR(15)  NOT NULL DEFAULT 'EN_ATTENTE'
                      CHECK (status IN ('EN_ATTENTE', 'EN_COURS', 'SUCCES', 'ECHEC')),
    started_at    TIMESTAMP,
    ended_at      TIMESTAMP,
    row_count     INTEGER,
    file_path     VARCHAR(500),
    error_message TEXT,
    triggered_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE extraction_destinataires (
    id        BIGSERIAL    PRIMARY KEY,
    config_id BIGINT       NOT NULL REFERENCES extraction_configs(id) ON DELETE CASCADE,
    email     VARCHAR(255) NOT NULL,
    nom       VARCHAR(100),
    active    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_extraction_jobs_config  ON extraction_jobs(config_id, created_at DESC);
CREATE INDEX idx_extraction_jobs_status  ON extraction_jobs(status);
CREATE INDEX idx_extraction_configs_inst ON extraction_configs(institution_id);
CREATE INDEX idx_extraction_configs_tmpl ON extraction_configs(template_id);
