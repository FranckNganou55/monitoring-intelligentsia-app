-- Explicit temporary test datasets, isolated from Oracle monitoring snapshots.
-- No fixtures are seeded by Flyway on any environment.
CREATE TABLE atm_test_cassette_fixtures (
    atm_id BIGINT PRIMARY KEY REFERENCES atms(id) ON DELETE CASCADE,
    dataset_code VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT atm_test_cassette_source_check CHECK (payload->>'source' = 'TEST_EXCEL'),
    CONSTRAINT atm_test_cassette_array_check CHECK (jsonb_typeof(payload->'cassettes') = 'array')
);
CREATE INDEX idx_atm_test_cassette_dataset ON atm_test_cassette_fixtures(dataset_code);
