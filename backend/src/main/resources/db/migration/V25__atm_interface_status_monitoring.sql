-- Historise les champs de la table SVFE INTERFACES pour les interfaces GAB.
-- Les valeurs sources restent textuelles afin de préserver les formats Oracle.
CREATE TABLE IF NOT EXISTS atm_interface_snapshots (
    id BIGSERIAL PRIMARY KEY,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    int_code TEXT,
    int_iden TEXT,
    int_labe TEXT,
    int_stat INTEGER,
    int_last_up_time TEXT,
    int_last_down_time TEXT,
    int_log_requ_in TEXT,
    int_log_requ_out TEXT,
    int_pid TEXT,
    int_prim_adre TEXT,
    int_prim_port TEXT,
    int_seco_adre TEXT,
    int_seco_port TEXT,
    int_numb_thre TEXT,
    int_numb_proc TEXT,
    int_flag_reco TEXT,
    int_numb_reco TEXT,
    int_dela_conn TEXT,
    int_time_out_acqu TEXT,
    int_time_out_issu TEXT,
    int_star_scri TEXT,
    int_stop_scri TEXT,
    int_trac_file TEXT,
    int_trac_leve TEXT,
    int_curr_file_ext TEXT,
    int_trac_refr_meth TEXT,
    int_trac_file_peri TEXT,
    int_trac_file_size TEXT,
    int_dest_stat_id TEXT,
    int_sour_stat_id TEXT,
    int_voix_debi TEXT,
    int_ity_code TEXT,
    int_ping_freq TEXT,
    int_supe_ping_flag TEXT,
    int_auto_star TEXT,
    int_auto_stop TEXT,
    int_stop_deci_freq TEXT,
    int_hsm_prod_type TEXT,
    int_hsm_veri_type TEXT,
    int_hky_code TEXT,
    int_pin_leng TEXT,
    int_ban_code TEXT,
    status_label TEXT
);

CREATE INDEX IF NOT EXISTS idx_atm_interface_snapshots_code_time
    ON atm_interface_snapshots (int_code, observed_at DESC);

CREATE TABLE IF NOT EXISTS atm_interface_monitoring_states (
    int_code TEXT PRIMARY KEY,
    interface_identifier TEXT,
    interface_label TEXT,
    current_status_code INTEGER,
    current_status_label TEXT,
    consecutive_status_code INTEGER,
    consecutive_samples INTEGER NOT NULL DEFAULT 0,
    outage_started_at TIMESTAMPTZ,
    last_sample_at TIMESTAMPTZ,
    alert_raised_for_outage BOOLEAN NOT NULL DEFAULT FALSE,
    active_alert_id BIGINT REFERENCES monitoring_alerts(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_atm_interface_state_last_sample
    ON atm_interface_monitoring_states (last_sample_at);

-- La collecte des interfaces GAB doit laisser trois minutes entre deux échantillons.
UPDATE scheduler_tasks
SET interval_ms = 180000
WHERE code = 'ATM_QUERY';

-- Une seule règle paramétrable dans l'écran Règles ; le risque reste modifiable.
INSERT INTO monitoring_rules (
    code, name, description, category, risk_level, enabled,
    notification_enabled, sound_enabled, visual_alert_enabled
)
SELECT
    'RULE_ATM_INTERFACE_UNAVAILABLE_5X',
    'Interface GAB DOWN ou SIGNOFF pendant 5 relevés',
    'Déclenche une alerte après cinq relevés consécutifs du même statut DOWN (0) ou SIGNOFF (3), espacés de trois minutes. Les statuts UP (1) et SIGNON (4) rétablissent la liaison.',
    'ATM', 'MOYEN', TRUE, TRUE, FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM monitoring_rules WHERE code = 'RULE_ATM_INTERFACE_UNAVAILABLE_5X'
);
