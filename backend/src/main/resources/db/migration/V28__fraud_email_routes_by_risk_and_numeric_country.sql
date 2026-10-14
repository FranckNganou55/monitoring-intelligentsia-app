-- Country identifiers are application-defined numeric keys, not calling prefixes.
ALTER TABLE institutions
    DROP CONSTRAINT IF EXISTS institutions_country_code_fkey;
ALTER TABLE institutions DROP COLUMN IF EXISTS country_code;
DROP TABLE IF EXISTS country_calling_codes CASCADE;

CREATE TABLE country_codes (
    code VARCHAR(20) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_country_code_numeric CHECK (code ~ '^[0-9]+$')
);

ALTER TABLE institutions
    ADD COLUMN country_numeric_code VARCHAR(20) REFERENCES country_codes(code) ON UPDATE CASCADE ON DELETE SET NULL,
    ADD COLUMN alert_email_typique VARCHAR(255),
    ADD COLUMN alert_email_moins_critique VARCHAR(255),
    ADD COLUMN alert_email_moyen VARCHAR(255),
    ADD COLUMN alert_email_critique VARCHAR(255);

CREATE INDEX idx_institutions_numeric_country_code
    ON institutions (country_numeric_code);
