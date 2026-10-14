-- V3 : Tables de snapshots monitoring (séries temporelles)

CREATE TABLE atm_snapshots (
    id                  BIGSERIAL   PRIMARY KEY,
    atm_id              BIGINT      NOT NULL REFERENCES atms(id) ON DELETE CASCADE,
    snapshot_time       TIMESTAMP   NOT NULL,
    currency1_id        BIGINT      REFERENCES currencies(id),
    note1               INTEGER,
    quantity1           INTEGER,
    currency2_id        BIGINT      REFERENCES currencies(id),
    note2               INTEGER,
    quantity2           INTEGER,
    currency3_id        BIGINT      REFERENCES currencies(id),
    note3               INTEGER,
    quantity3           INTEGER,
    currency4_id        BIGINT      REFERENCES currencies(id),
    note4               INTEGER,
    quantity4           INTEGER,
    status_id           VARCHAR(10),
    status_desc         VARCHAR(200),
    all_ok              BOOLEAN,
    status_message      TEXT,
    threshold1_exceeded BOOLEAN     NOT NULL DEFAULT FALSE,
    threshold2_exceeded BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_atm_snaps_atm_time ON atm_snapshots(atm_id, snapshot_time DESC);
CREATE INDEX idx_atm_snaps_time     ON atm_snapshots(snapshot_time DESC);

CREATE TABLE network_snapshots (
    id                BIGSERIAL    PRIMARY KEY,
    network_code      VARCHAR(50)  NOT NULL,
    description       VARCHAR(200),
    operating_status  VARCHAR(20),
    processing_status VARCHAR(20),
    transaction_mode  VARCHAR(20),
    snapshot_time     TIMESTAMP    NOT NULL
);
CREATE INDEX idx_net_snaps_time ON network_snapshots(snapshot_time DESC);

CREATE TABLE host_snapshots (
    id                BIGSERIAL    PRIMARY KEY,
    hostname          VARCHAR(200) NOT NULL,
    ip_address        VARCHAR(50),
    operating_status  VARCHAR(20),
    processing_status VARCHAR(20),
    snapshot_time     TIMESTAMP    NOT NULL
);
CREATE INDEX idx_host_snaps_time ON host_snapshots(snapshot_time DESC);

CREATE TABLE transaction_rates (
    id            BIGSERIAL     PRIMARY KEY,
    network       VARCHAR(50)   NOT NULL,
    acquirer      VARCHAR(50),
    issuer        VARCHAR(50),
    success_count INTEGER       NOT NULL DEFAULT 0,
    success_rate  NUMERIC(5, 2),
    failure_count INTEGER       NOT NULL DEFAULT 0,
    failure_rate  NUMERIC(5, 2),
    snapshot_time TIMESTAMP     NOT NULL
);
CREATE INDEX idx_tx_rates_time ON transaction_rates(snapshot_time DESC);

CREATE TABLE transaction_errors (
    id            BIGSERIAL    PRIMARY KEY,
    network       VARCHAR(50)  NOT NULL,
    response_code VARCHAR(10),
    response_desc VARCHAR(200),
    count         INTEGER      NOT NULL DEFAULT 0,
    snapshot_time TIMESTAMP    NOT NULL
);
CREATE INDEX idx_tx_errors_time ON transaction_errors(snapshot_time DESC);

CREATE TABLE fraud_rates (
    id             BIGSERIAL   PRIMARY KEY,
    institution_id BIGINT      REFERENCES institutions(id) ON DELETE SET NULL,
    bin            VARCHAR(10) NOT NULL,
    suspect_count  INTEGER     NOT NULL DEFAULT 0,
    fraud_count    INTEGER     NOT NULL DEFAULT 0,
    snapshot_time  TIMESTAMP   NOT NULL
);
CREATE INDEX idx_fraud_rates_time ON fraud_rates(snapshot_time DESC);

CREATE TABLE fraud_details (
    id             BIGSERIAL    PRIMARY KEY,
    institution_id BIGINT       REFERENCES institutions(id) ON DELETE SET NULL,
    bin            VARCHAR(10)  NOT NULL,
    rule_code      VARCHAR(50),
    rule_desc      VARCHAR(200),
    suspect_count  INTEGER      NOT NULL DEFAULT 0,
    fraud_count    INTEGER      NOT NULL DEFAULT 0,
    bins           TEXT,
    description    TEXT,
    snapshot_time  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_fraud_details_time ON fraud_details(snapshot_time DESC);

CREATE TABLE fleet_accounts (
    id               BIGSERIAL      PRIMARY KEY,
    institution_id   BIGINT         REFERENCES institutions(id) ON DELETE SET NULL,
    account_number   VARCHAR(50)    NOT NULL,
    currency_id      BIGINT         REFERENCES currencies(id),
    balance          NUMERIC(20, 2),
    previous_balance NUMERIC(20, 2),
    delta1           NUMERIC(20, 2),
    delta2           NUMERIC(20, 2),
    delta1_exceeded  BOOLEAN        NOT NULL DEFAULT FALSE,
    delta2_exceeded  BOOLEAN        NOT NULL DEFAULT FALSE,
    snapshot_time    TIMESTAMP      NOT NULL
);
CREATE INDEX idx_fleet_time ON fleet_accounts(snapshot_time DESC);

CREATE TABLE cards_over_limit (
    id             BIGSERIAL      PRIMARY KEY,
    institution_id BIGINT         REFERENCES institutions(id) ON DELETE SET NULL,
    card_number    VARCHAR(30)    NOT NULL,
    currency_id    BIGINT         REFERENCES currencies(id),
    limit_amount   NUMERIC(20, 2),
    current_amount NUMERIC(20, 2),
    exceeded       BOOLEAN        NOT NULL DEFAULT FALSE,
    snapshot_time  TIMESTAMP      NOT NULL
);
CREATE INDEX idx_cards_limit_time ON cards_over_limit(snapshot_time DESC);

CREATE TABLE pos_telecollect_snapshots (
    id             BIGSERIAL   PRIMARY KEY,
    institution_id BIGINT      REFERENCES institutions(id) ON DELETE SET NULL,
    terminal_id    VARCHAR(50) NOT NULL,
    collect_date   DATE,
    status         VARCHAR(20),
    snapshot_time  TIMESTAMP   NOT NULL
);
CREATE INDEX idx_telecollect_time ON pos_telecollect_snapshots(snapshot_time DESC);
