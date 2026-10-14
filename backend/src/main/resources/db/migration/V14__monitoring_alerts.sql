-- V14__monitoring_alerts.sql

CREATE TABLE monitoring_alerts (
    id BIGSERIAL PRIMARY KEY,

    rule_id BIGINT NOT NULL
        REFERENCES monitoring_rules(id)
        ON DELETE RESTRICT,

    category VARCHAR(20) NOT NULL
        CHECK (category IN ('FRAUD', 'ATM', 'NETWORK')),

    risk_level VARCHAR(30) NOT NULL
        CHECK (risk_level IN (
            'TYPIQUE',
            'MOINS_CRITIQUE',
            'MOYEN',
            'CRITIQUE'
        )),

    title VARCHAR(200) NOT NULL,

    description TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN (
            'OPEN',
            'ACKNOWLEDGED',
            'IN_PROGRESS',
            'RESOLVED',
            'ARCHIVED'
        )),

    institution_id BIGINT
        REFERENCES institutions(id)
        ON DELETE SET NULL,

    atm_id BIGINT
        REFERENCES atms(id)
        ON DELETE SET NULL,

    fraud_detection_id BIGINT,

    transaction_reference VARCHAR(100),

    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),

    acknowledged_at TIMESTAMP,

    resolved_at TIMESTAMP,

    archived_at TIMESTAMP,

    deleted_at TIMESTAMP,

    assigned_to BIGINT
        REFERENCES users(id)
        ON DELETE SET NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monitoring_alerts_rule
    ON monitoring_alerts(rule_id);

CREATE INDEX idx_monitoring_alerts_category
    ON monitoring_alerts(category);

CREATE INDEX idx_monitoring_alerts_risk_level
    ON monitoring_alerts(risk_level);

CREATE INDEX idx_monitoring_alerts_status
    ON monitoring_alerts(status);

CREATE INDEX idx_monitoring_alerts_detected_at
    ON monitoring_alerts(detected_at);

CREATE INDEX idx_monitoring_alerts_institution
    ON monitoring_alerts(institution_id);

CREATE INDEX idx_monitoring_alerts_atm
    ON monitoring_alerts(atm_id);

CREATE INDEX idx_monitoring_alerts_assigned_to
    ON monitoring_alerts(assigned_to);


CREATE TABLE alert_events (
    id BIGSERIAL PRIMARY KEY,

    alert_id BIGINT NOT NULL
        REFERENCES monitoring_alerts(id)
        ON DELETE CASCADE,

    event_type VARCHAR(30) NOT NULL
        CHECK (event_type IN (
            'CREATED',
            'ACKNOWLEDGED',
            'ASSIGNED',
            'COMMENTED',
            'IN_PROGRESS',
            'RESOLVED',
            'ARCHIVED',
            'REOPENED'
        )),

    actor_user_id BIGINT
        REFERENCES users(id)
        ON DELETE SET NULL,

    old_status VARCHAR(30),

    new_status VARCHAR(30),

    comment TEXT,

    details JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_events_alert
    ON alert_events(alert_id);

CREATE INDEX idx_alert_events_created_at
    ON alert_events(created_at);

CREATE INDEX idx_alert_events_type
    ON alert_events(event_type);