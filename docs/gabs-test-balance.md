# GABs de test issus de BALANCE.xlsx

Les 9 GABs et leurs 36 cassettes sont ajoutés uniquement dans la base locale `monitoring_test`. Les 4 GABs de test existants restent présents. Le marquage `TEST-BALANCE-` apparaît dans leur identifiant et `[TEST BALANCE.xlsx]` dans leur adresse. Le fichier ne fournit aucun statut opérationnel. Une répartition synthétique a ensuite été explicitement autorisée : 3 SIGNON (opérationnels), 2 UP et 2 SIGNOFF (avertissement), 2 DOWN (hors ligne).

Les nouvelles cassettes conservent exactement les valeurs Excel, y compris les 8 cassettes aux compteurs/solde absents et les dates distinctes. Les cellules vides sont enregistrées comme `null`.

| Institution | GAB de test | Nom fourni dans Excel |
|---|---|---|
| 00043 | TEST-BALANCE-108401001 | GAB N1 HAI CINEMA |
| 00043 | TEST-BALANCE-108401002 | GAB N2 HAI CINEMA |
| 00001 | TEST-BALANCE-108401003 | GAB N1 AIRPORT |
| 00043 | TEST-BALANCE-108401004 | ATM N1 SHERIKAT |
| 00001 | TEST-BALANCE-108401005 | ATM N2 SHERIKAT |
| 00043 | TEST-BALANCE-108401006 | ATM N3 SHERIKAT |
| 00001 | TEST-BALANCE-108401007 | ATM GUDELE 21 |
| 00043 | TEST-BALANCE-108401008 | ATM GUDELE 22 |
| 00001 | TEST-BALANCE-108401009 | ATM KONYO KONYO |

Totaux avec les GABs précédents : **00001 : 7 GABs**, **00043 : 6 GABs**.

## Affichage et isolation

Ouvrir la page de détail de chaque `TEST-BALANCE-...`. Le tableau annonce « Jeu de test BALANCE.xlsx » et l’API retourne `source: "TEST_EXCEL"`. Les données sont stockées dans `atm_test_cassette_fixtures`, jamais dans Oracle ni dans ses tables de relevés. Le lien est l’identifiant du GAB local. Les GABs réels continuent de lire Oracle et retournent `source: "ORACLE"`. Une panne Oracle ne provoque aucun remplacement par des fixtures.

La migration V31 crée uniquement la structure vide de stockage des tests. Elle ne crée aucun GAB sur une autre base. Les insertions sont réservées explicitement à `monitoring_test`.

## Recréer et supprimer plus tard

Le script de création est idempotent : il peut être rejoué sans dupliquer les 9 GABs. Les commandes utilisent le compte PostgreSQL du projet; définir `PGPASSWORD` dans le terminal ou laisser psql demander le mot de passe.

```powershell
# Création autorisée du jeu local, après application de V31 :
& "C:/Program Files/PostgreSQL/16/bin/psql.exe" -h localhost -U neo-monitoring -d monitoring_test -v ON_ERROR_STOP=1 -f backend/scripts/test-data/seed-balance-gabs.sql

# À exécuter uniquement une fois la suppression du jeu décidée :
& "C:/Program Files/PostgreSQL/16/bin/psql.exe" -h localhost -U neo-monitoring -d monitoring_test -v ON_ERROR_STOP=1 -f backend/scripts/test-data/remove-balance-gabs.sql
```

La suppression cible uniquement ce jeu par son `dataset_code`, son lien explicite et le préfixe de ses terminaux. Les 36 cassettes sont supprimées en cascade avec leurs GABs. Les quatre anciens GABs de test et les relevés Oracle sont conservés. Le script de suppression n’a pas été exécuté.

## Statuts de test autorisés

Le script `backend/scripts/test-data/seed-balance-interface-statuses.sql` ajoute 9 interfaces marquées `TEST_BALANCE_...` et les observations correspondantes dans l’historique local des GABs. Il ne modifie ni les cassettes Excel ni Oracle. Les numéros finissant par 001, 005, 009 sont SIGNON; 002 et 006 sont UP; 003 et 007 sont SIGNOFF; 004 et 008 sont DOWN. Le script de suppression retire aussi les observations des interfaces de ce jeu.
