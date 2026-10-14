-- V4 : Tables d'extraction ATM et POS

CREATE TABLE extraction_batches (
    id             BIGSERIAL    PRIMARY KEY,
    institution_id BIGINT       NOT NULL REFERENCES institutions(id),
    type           VARCHAR(10)  NOT NULL CHECK (type IN ('ATM', 'POS')),
    start_date     TIMESTAMP    NOT NULL,
    end_date       TIMESTAMP    NOT NULL,
    merchant       VARCHAR(200),
    city           VARCHAR(100),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT       NOT NULL,
    CONSTRAINT chk_extraction_dates CHECK (end_date >= start_date)
);
CREATE INDEX idx_extraction_batches_inst ON extraction_batches(institution_id);
CREATE INDEX idx_extraction_batches_date ON extraction_batches(created_at DESC);

CREATE TABLE extraction_terminals (
    id          BIGSERIAL   PRIMARY KEY,
    batch_id    BIGINT      NOT NULL REFERENCES extraction_batches(id) ON DELETE CASCADE,
    terminal_id VARCHAR(50) NOT NULL
);
CREATE INDEX idx_ext_terminals_batch ON extraction_terminals(batch_id);

CREATE TABLE extraction_transactions (
    id               BIGSERIAL      PRIMARY KEY,
    batch_id         BIGINT         NOT NULL REFERENCES extraction_batches(id) ON DELETE CASCADE,
    terminal_id      VARCHAR(50),
    transaction_date TIMESTAMP,
    amount           NUMERIC(20, 2),
    response_code    VARCHAR(10),
    status           VARCHAR(20),
    merchant         VARCHAR(200),
    city             VARCHAR(100)
);
CREATE INDEX idx_ext_transactions_batch ON extraction_transactions(batch_id);
CREATE INDEX idx_ext_transactions_date  ON extraction_transactions(transaction_date DESC);
