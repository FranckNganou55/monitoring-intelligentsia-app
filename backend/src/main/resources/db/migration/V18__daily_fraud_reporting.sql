-- V18__daily_fraud_reporting.sql

-- Étendre les formats de sortie des extractions.
-- V9 autorise actuellement : CSV, EXCEL, PDF.
-- V18 ajoute le format WORD.

ALTER TABLE extraction_configs
DROP CONSTRAINT IF EXISTS extraction_configs_output_format_check;

ALTER TABLE extraction_configs
ADD CONSTRAINT chk_extraction_config_output_format
CHECK (
    output_format IN (
        'CSV',
        'EXCEL',
        'PDF',
        'WORD'
    )
);


-- Rapports journaliers de fraude

CREATE TABLE fraud_daily_reports (
    id BIGSERIAL PRIMARY KEY,

    report_date DATE NOT NULL,

    total_detected INTEGER NOT NULL DEFAULT 0,

    total_processed INTEGER NOT NULL DEFAULT 0,

    total_unprocessed INTEGER NOT NULL DEFAULT 0,

    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    status VARCHAR(30) NOT NULL
        CHECK (status IN (
            'GENERATED',
            'PARTIAL',
            'FAILED'
        )),

    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_fraud_daily_reports_date
    ON fraud_daily_reports(report_date);