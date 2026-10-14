-- V16__fraud_detections.sql

CREATE TABLE fraud_detections (
    id BIGSERIAL PRIMARY KEY,

    rule_id BIGINT NOT NULL
        REFERENCES monitoring_rules(id)
        ON DELETE RESTRICT,

    institution_id BIGINT
        REFERENCES institutions(id)
        ON DELETE SET NULL,

    risk_auth_code VARCHAR(100),

    risk_rule_code VARCHAR(100) NOT NULL,

    auth_code VARCHAR(100),

    account_reference VARCHAR(150),

    transaction_type VARCHAR(50),

    amount NUMERIC(19,4),

    currency VARCHAR(10),

    merchant_type VARCHAR(20),

    merchant_name VARCHAR(255),

    country_code VARCHAR(10),

    retrieval_reference VARCHAR(100),

    authorization_response VARCHAR(100),

    source_risk_level VARCHAR(50),

    application_risk_level VARCHAR(30) NOT NULL
        CHECK (application_risk_level IN (
            'TYPIQUE',
            'MOINS_CRITIQUE',
            'MOYEN',
            'CRITIQUE'
        )),

    status VARCHAR(30) NOT NULL DEFAULT 'DETECTED'
        CHECK (status IN (
            'DETECTED',
            'UNDER_REVIEW',
            'CONFIRMED',
            'FALSE_POSITIVE',
            'RESOLVED'
        )),

    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),

    resolved_at TIMESTAMP,

    status_date TIMESTAMP,

    status_maker VARCHAR(150),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_detections_rule
    ON fraud_detections(rule_id);

CREATE INDEX idx_fraud_detections_institution
    ON fraud_detections(institution_id);

CREATE INDEX idx_fraud_detections_rule_code
    ON fraud_detections(risk_rule_code);

CREATE INDEX idx_fraud_detections_risk_level
    ON fraud_detections(application_risk_level);

CREATE INDEX idx_fraud_detections_status
    ON fraud_detections(status);

CREATE INDEX idx_fraud_detections_detected_at
    ON fraud_detections(detected_at);