-- V25 already stores interface observations; V29 configures sampling cadence.
-- Applied migrations are preserved. Prefer interface status over legacy terminal health.
CREATE VIEW atm_display_status AS
SELECT a.id AS atm_id, a.institution_id,
       COALESCE(i.int_stat::text, NULLIF(s.status_id,''), s.status_desc) AS status_id,
       CASE i.int_stat WHEN 0 THEN 'DOWN' WHEN 1 THEN 'UP' WHEN 3 THEN 'SIGNOFF' WHEN 4 THEN 'SIGNON'
            ELSE COALESCE(i.status_label,s.status_desc) END AS status_desc,
       COALESCE(i.observed_at,s.snapshot_time AT TIME ZONE 'UTC') AS observed_at,
       CASE
         WHEN i.id IS NOT NULL THEN CASE i.int_stat WHEN 0 THEN 'error' WHEN 1 THEN 'warning' WHEN 3 THEN 'warning' WHEN 4 THEN 'ok' ELSE 'unknown' END
         WHEN UPPER(COALESCE(NULLIF(s.status_id,''),s.status_desc,'')) IN ('0','DOWN','OFFLINE','OUTOFSERVICE','OUT_OF_SERVICE') THEN 'error'
         WHEN UPPER(COALESCE(NULLIF(s.status_id,''),s.status_desc,'')) IN ('1','UP','3','SIGNOFF','SIGN_OFF','WARN','WARNING') THEN 'warning'
         WHEN UPPER(COALESCE(NULLIF(s.status_id,''),s.status_desc,'')) IN ('4','SIGNON','SIGN_ON','OK','ONLINE','INSERVICE','IN_SERVICE','ACTIVE') THEN 'ok'
         ELSE 'unknown' END AS status_key
FROM atms a
LEFT JOIN atm_test_cassette_fixtures f ON f.atm_id=a.id
LEFT JOIN LATERAL (
    SELECT x.* FROM atm_interface_snapshots x
    WHERE regexp_replace(UPPER(TRIM(x.int_iden)), '^ATM_', '') = UPPER(COALESCE(f.payload->>'numeroGab',a.terminal_id))
       OR regexp_replace(UPPER(TRIM(x.int_labe)), '^ATM_', '') = UPPER(COALESCE(f.payload->>'numeroGab',a.terminal_id))
    ORDER BY x.observed_at DESC,x.id DESC LIMIT 1
) i ON TRUE
LEFT JOIN LATERAL (SELECT x.* FROM atm_snapshots x WHERE x.atm_id=a.id ORDER BY x.snapshot_time DESC,x.id DESC LIMIT 1) s ON TRUE;
