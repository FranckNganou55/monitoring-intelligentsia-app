# Cassettes des GABs — Oracle et page de détail

L’implémentation utilise Spring Boot 3.4.5 / Java 17, React / TypeScript / TanStack Query et le client Axios authentifié existant. Les tables sont lues directement avec le bean `svfeJdbcTemplate`, déjà utilisé par les repositories Oracle du projet. Le chemin Oracle ne crée aucun relevé et n’importe pas le fichier Excel. À la demande explicite suivante, un jeu local de GABs de test a été ajouté séparément; consulter [le guide du jeu de test](gabs-test-balance.md).

Le premier onglet de BALANCE.xlsx contient 36 lignes de cassettes. Le fichier sert de référence des colonnes pour Oracle et de source du jeu de test explicitement demandé. `ABD_CUR_CODE` est conservé comme code source (par exemple `28`), sans lui attribuer arbitrairement une devise ISO.

## Endpoint et restrictions

- `GET /api/gabs/cassettes` : derniers relevés regroupés par GAB, pour le contexte autorisé.
- `GET /api/gabs/cassettes?atmId=3` : cassettes du GAB dont **l’identifiant local** est 3.
- `institutionId` : filtre facultatif pour un utilisateur global. Pour un utilisateur institutionnel, l’institution de son compte est imposée par le serveur.
- Authentification Bearer et permission `ATM_READ` obligatoires.
- Le lien entre le GAB local et Oracle est `atms.svfe_pid = ATM_TERMINAL.ATE_CODE`. Le numéro du GAB reste une chaîne afin de préserver les zéros initiaux.
- GAB absent ou appartenant à une autre institution : HTTP 404. Lien `svfe_pid` absent : HTTP 409, code `ATM_NOT_LINKED`.
- Oracle désactivé/inaccessible ou lecture refusée : HTTP 503, code `ORACLE_CASSETTES_UNAVAILABLE`. Une erreur n’est jamais remplacée par des données fictives ou un succès vide.
- Aucun relevé correspondant : HTTP 200 et `data: []`.

## Structure exacte du JSON

Voici un exemple de structure, avec les valeurs de la première cassette du premier onglet Excel. L’API ne renvoie ces valeurs que si la requête Oracle les renvoie réellement. Le timestamp d’enveloppe dépend du moment de la requête.

```json
{
  "success": true,
  "data": [
    {
      "codeGab": 1067780,
      "numeroGab": "108401001",
      "nomGab": "GAB N1 HAI CINEMA",
      "source": "ORACLE",
      "cassettes": [
        {
          "cassetteNum": 1,
          "devise": "28",
          "valeurBillet": 100,
          "billetsRestants": 244,
          "billetsRejetes": 9,
          "billetsDispenses": 47,
          "soldeCassette": 24400,
          "derniereMaj": "2025-06-21T08:44:35"
        }
      ]
    }
  ],
  "timestamp": "2026-10-12T10:00:00Z"
}
```

Les autres cassettes suivent dans la même liste, puis les autres GABs. `valeurBillet`, les trois compteurs, `soldeCassette`, `cassetteNum`, `devise` et `derniereMaj` peuvent être `null`; ces propriétés restent présentes dans le JSON. Un zéro réel reste `0`. Le solde provient de la multiplication SQL, avec `BigDecimal` côté Java et sans `NVL`/`COALESCE` qui transformerait une absence en zéro.

`derniereMaj` provient du timestamp JDBC, sérialisé en ISO 8601. Un DATE Oracle ne contient pas de fuseau : l’API utilise `LocalDateTime`, sans inventer un suffixe UTC `Z`. Le frontend affiche la date au format `dd/MM/yyyy HH:mm:ss`.

Les alias SQL sont exactement `CODE_GAB`, `NUMERO_GAB`, `NOM_GAB`, `CASSETTE_NUM`, `DEVISE`, `VALEUR_BILLET`, `BILLETS_RESTANTS`, `BILLETS_REJETES`, `BILLETS_DISPENSES`, `SOLDE_CASSETTE`, `DERNIERE_MAJ`. Le JSON utilise les noms camelCase des DTO. La sélection MAX porte sur chaque couple GAB/cassette, pas sur une date commune à tout le GAB. La requête conserve le comportement demandé en cas de plusieurs lignes partageant exactement le dernier timestamp : toutes ces lignes sont retournées.

## Frontend

La page existante `/atms/:id` affiche le nouveau composant `AtmCassettes` après les informations du GAB, avant son historique de mouvements. Il remplace les anciennes cartes de quantités provenant des snapshots locaux. Le statut et le message du GAB restent issus de leur surveillance existante.

Le service `gabCassetteService.list({ atmId })` utilise le client Axios du projet : URL `/api/gabs/cassettes`, token Bearer et filtre d’institution existants. Le composant gère chargement, erreur avec message du serveur et bouton Actualiser, absence de relevé, puis tableau des cassettes. Rafraîchissement toutes les 30 secondes; une donnée absente s’affiche « — » et aucune estimation de devise n’est ajoutée.

## Connexion et lancement — PowerShell

Depuis le dossier racine du projet, renseigner les **valeurs réelles de votre environnement** dans le même terminal que Maven. Le mot de passe n’est pas inscrit dans le projet :

```powershell
$env:SVFE_ENABLED = 'true'
$env:SVFE_DB_URL = 'jdbc:oracle:thin:@//SERVEUR_ORACLE:1521/SERVICE_ORACLE'
$env:SVFE_DB_USER = 'UTILISATEUR_ORACLE'
$taskOracleSecret = Read-Host 'Mot de passe Oracle' -AsSecureString
$env:SVFE_DB_PASS = [System.Net.NetworkCredential]::new('', $taskOracleSecret).Password
# Facultatif : schéma propriétaire si différent du compte connecté.
$env:SVFE_DB_SCHEMA = 'SCHEMA_ORACLE'
$env:CORS_ORIGINS = 'http://localhost:5173,http://127.0.0.1:5173'
mvn -f backend/pom.xml spring-boot:run
```

Remplacer les placeholders par la configuration Oracle existante. Si les tables sont accessibles dans le schéma courant ou via synonymes, ne pas définir `SVFE_DB_SCHEMA`. Le compte doit pouvoir lire `ATM_TERMINAL` et `ATM_BALANCE_DETALS`; aucune permission d’écriture n’est nécessaire pour cette fonctionnalité. Les autres traitements Oracle existants restent soumis à leurs droits habituels.

Dans un deuxième terminal :

```powershell
npm --prefix frontend install
npm --prefix frontend run dev
```

Le backend local écoute actuellement sur 8089; Vite sur 5173. Vite utilise déjà le proxy `/api`; pour un frontend hébergé séparément, configurer son `VITE_API_BASE_URL` et ajouter son origine exacte dans `CORS_ORIGINS`. La configuration CORS existante autorise GET/OPTIONS et l’en-tête Authorization; elle n’ouvre pas l’API à toutes les origines.

## Tester

1. Se connecter à l’application avec un utilisateur autorisé à consulter les GABs.
2. Ouvrir le détail d’un GAB réellement synchronisé (`svfe_pid` renseigné).
3. Vérifier dans Réseau du navigateur le GET `/api/gabs/cassettes?atmId=...` et comparer ses cassettes au résultat de la requête Oracle.
4. Vérifier une cassette avec valeurs absentes, une cassette avec zéro et un GAB sans relevé. Chaque cassette affiche sa propre date.
5. Vérifier qu’un utilisateur d’une autre institution ne peut pas consulter le GAB.

Exemple d’appel manuel avec un token obtenu par l’authentification habituelle :

```powershell
$taskToken = Read-Host 'Token Bearer'
Invoke-RestMethod -Uri 'http://localhost:8089/api/gabs/cassettes?atmId=3' -Headers @{ Authorization = "Bearer $taskToken" }
```

Tests automatisés (PostgreSQL local de test, tables TEMP et rollback pour vérifier le SQL sans toucher aux tables métier) :

```powershell
mvn -f backend/pom.xml test '-Dtest=GabCassetteServiceTest,GabCassetteRepositoryTest,MonitoringEndpointsTest' '-Dmonitoring.dbTests=true'
npm --prefix frontend run build
```

La connexion à votre Oracle réel et le mapping de ses GABs ne peuvent pas être certifiés par les tests PostgreSQL. Une validation finale sur Oracle est nécessaire après activation de sa connexion.

## Jeu de test explicitement demandé

Pour un `atmId` enregistré dans `atm_test_cassette_fixtures`, les données de BALANCE.xlsx sont renvoyées avec `source: "TEST_EXCEL"`, après contrôle de l’institution. Tous les autres GABs utilisent exclusivement Oracle (`source: "ORACLE"`). GET sans atmId continue de consulter Oracle; il ne se transforme pas en liste de test lorsque la connexion est indisponible. V31 crée une table locale de tests vide, sans insertion automatique.
