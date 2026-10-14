CREATE TABLE country_calling_codes (
    code        VARCHAR(8) PRIMARY KEY,
    country_name VARCHAR(100) NOT NULL,
    iso_code    VARCHAR(2) NOT NULL UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_country_calling_code_format CHECK (code ~ '^[+][1-9][0-9]{0,3}$'),
    CONSTRAINT chk_country_iso_code_format CHECK (iso_code ~ '^[A-Z]{2}$')
);

INSERT INTO country_calling_codes (code, country_name, iso_code) VALUES
    ('+225', 'Côte d’Ivoire', 'CI'),
    ('+237', 'Cameroun', 'CM'),
    ('+221', 'Sénégal', 'SN'),
    ('+234', 'Nigeria', 'NG'),
    ('+228', 'Togo', 'TG'),
    ('+226', 'Burkina Faso', 'BF'),
    ('+224', 'Guinée', 'GN'),
    ('+223', 'Mali', 'ML'),
    ('+229', 'Bénin', 'BJ'),
    ('+233', 'Ghana', 'GH'),
    ('+243', 'République démocratique du Congo', 'CD'),
    ('+242', 'République du Congo', 'CG'),
    ('+241', 'Gabon', 'GA'),
    ('+27',  'Afrique du Sud', 'ZA'),
    ('+33',  'France', 'FR'),
    ('+1',   'États-Unis / Canada', 'US')
ON CONFLICT (code) DO NOTHING;

ALTER TABLE institutions
    ADD COLUMN alert_email VARCHAR(255),
    ADD COLUMN alert_email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN country_code VARCHAR(8) REFERENCES country_calling_codes(code) ON UPDATE CASCADE ON DELETE SET NULL;

CREATE INDEX idx_institutions_alert_email_enabled
    ON institutions (id) WHERE alert_email_enabled = TRUE;
