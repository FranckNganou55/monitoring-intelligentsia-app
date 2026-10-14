ALTER TABLE host_snapshots
    ADD COLUMN IF NOT EXISTS description VARCHAR(200);
