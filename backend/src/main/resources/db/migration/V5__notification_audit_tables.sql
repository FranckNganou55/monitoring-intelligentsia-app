-- V5 : Tables de notifications in-app et journal d'audit

CREATE TABLE notifications (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     TEXT,
    read        BOOLEAN      NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(50),
    entity_id   BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_user_read ON notifications(user_id, read, created_at DESC);
CREATE INDEX idx_notif_time      ON notifications(created_at DESC);

CREATE TABLE audit_logs (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT,
    user_login  VARCHAR(100),
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(100),
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_user_time   ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_time        ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_action      ON audit_logs(action);
