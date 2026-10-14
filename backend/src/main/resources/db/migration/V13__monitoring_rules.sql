CREATE TABLE monitoring_rules (
    id                    BIGSERIAL PRIMARY KEY,
    code                  VARCHAR(50)  NOT NULL UNIQUE,
    name                  VARCHAR(200) NOT NULL,
    description           TEXT,
    category              VARCHAR(20)  NOT NULL
        CHECK (category IN ('FRAUD', 'ATM', 'NETWORK')),
    risk_level             VARCHAR(30)  NOT NULL DEFAULT 'MOYEN'
        CHECK (risk_level IN (
            'TYPIQUE',
            'MOINS_CRITIQUE',
            'MOYEN',
            'CRITIQUE'
        )),
    enabled                BOOLEAN NOT NULL DEFAULT TRUE,
    notification_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    sound_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    visual_alert_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by             BIGINT REFERENCES users(id) ON DELETE SET NULL,
    updated_by             BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_monitoring_rules_category
    ON monitoring_rules(category);

CREATE INDEX idx_monitoring_rules_risk_level
    ON monitoring_rules(risk_level);

CREATE INDEX idx_monitoring_rules_enabled
    ON monitoring_rules(enabled);