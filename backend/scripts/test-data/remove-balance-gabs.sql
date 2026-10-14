-- À exécuter seulement quand la suppression du jeu est demandée.
BEGIN;
DO $$ BEGIN IF current_database() <> 'monitoring_test' THEN RAISE EXCEPTION 'Base de test requise'; END IF; END $$;
DELETE FROM atm_interface_snapshots s USING atm_test_cassette_fixtures f
WHERE f.dataset_code='BALANCE_XLSX_20261012'
  AND s.int_code='TEST_BALANCE_' || (f.payload->>'numeroGab');
DELETE FROM atms a USING atm_test_cassette_fixtures f
WHERE f.atm_id=a.id AND f.dataset_code='BALANCE_XLSX_20261012'
  AND a.terminal_id LIKE 'TEST-BALANCE-%';
-- Les cassettes du jeu sont supprimées par ON DELETE CASCADE.
COMMIT;
