-- V15__atm_network_monitoring_states.sql

CREATE TABLE atm_monitoring_states (
    id BIGSERIAL PRIMARY KEY,

    atm_id BIGINT NOT NULL UNIQUE
        REFERENCES atms(id)
        ON DELETE CASCADE,

    current_status_code INTEGER,
    current_status_label VARCHAR(30),

    previous_status_code INTEGER,
    previous_status_label VARCHAR(30),

    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    consecutive_successes INTEGER NOT NULL DEFAULT 0,

    check_window_started_at TIMESTAMP,

    last_check_at TIMESTAMP,
    last_down_at TIMESTAMP,
    last_up_at TIMESTAMP,
    last_signoff_at TIMESTAMP,
    last_signon_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_atm_monitoring_state_status
    ON atm_monitoring_states(current_status_code);

CREATE INDEX idx_atm_monitoring_state_last_check
    ON atm_monitoring_states(last_check_at);


CREATE TABLE network_monitoring_states (
    id BIGSERIAL PRIMARY KEY,

    interface_code VARCHAR(100) NOT NULL UNIQUE,

    interface_identifier VARCHAR(200),

    current_status_code INTEGER,
    current_status_label VARCHAR(30),

    previous_status_code INTEGER,
    previous_status_label VARCHAR(30),

    consecutive_down INTEGER NOT NULL DEFAULT 0,
    consecutive_up INTEGER NOT NULL DEFAULT 0,
    consecutive_signoff INTEGER NOT NULL DEFAULT 0,
    consecutive_signon INTEGER NOT NULL DEFAULT 0,

    window_started_at TIMESTAMP,

    last_check_at TIMESTAMP,
    last_down_at TIMESTAMP,
    last_up_at TIMESTAMP,
    last_signoff_at TIMESTAMP,
    last_signon_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_network_state_status
    ON network_monitoring_states(current_status_code);

CREATE INDEX idx_network_state_last_check
    ON network_monitoring_states(last_check_at);


CREATE TABLE network_check_windows (
    id BIGSERIAL PRIMARY KEY,

    network_state_id BIGINT NOT NULL
        REFERENCES network_monitoring_states(id)
        ON DELETE CASCADE,

    started_at TIMESTAMP NOT NULL,

    ended_at TIMESTAMP,

    expected_checks INTEGER NOT NULL DEFAULT 5,

    completed_checks INTEGER NOT NULL DEFAULT 0,

    failed_checks INTEGER NOT NULL DEFAULT 0,

    successful_checks INTEGER NOT NULL DEFAULT 0,

    signoff_checks INTEGER NOT NULL DEFAULT 0,

    signon_checks INTEGER NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN (
            'IN_PROGRESS',
            'STABLE',
            'FAILED',
            'RECOVERED'
        )),

    alert_id BIGINT
        REFERENCES monitoring_alerts(id)
        ON DELETE SET NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_network_check_windows_state
    ON network_check_windows(network_state_id);

CREATE INDEX idx_network_check_windows_status
    ON network_check_windows(status);

CREATE INDEX idx_network_check_windows_started
    ON network_check_windows(started_at);