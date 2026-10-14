# Analyse de l'Ancien Projet — Monitoring Plateforme Monétique

> **Lecture seule** — aucun fichier de l'ancien projet n'a été modifié.  
> Répertoire source : `../monitoringplateformemonetique-main/monitoringplateformemonetique-main`  
> Date d'analyse : 2026-04-30

---

## 1. Structure Générale

### Nom et Objectif

**Projet :** Monitoring Plateforme Monétique  
**Objectif :** Plateforme de surveillance en temps réel d'un réseau monétique (GABs/ATMs, TPE/POS, transactions, détection de fraude, gestion de flotte).  
**Type :** Application Java 8 multi-modules Maven, déployée sur Tomcat 8/9.

### Structure des Modules

```
framework-parent/                    (POM parent — versions, plugins)
├── framework-utilities/             (Entités de base, mail, parsing de fichiers)
├── framework-security/              (Spring Security, UserDetailsService, BCrypt)
├── framework-core/                  (Entités framework : menus, portails, institutions, services)
├── framework-jsf/                   (Backing beans JSF abstraits, AbstractMainBean)
├── monitoring/                      (WAR principal — toute la logique métier monitoring)
└── notification/                    (WAR secondaire — génération Excel, alertes, sous-développé)
```

### Stack Technique

| Couche | Technologies |
|--------|-------------|
| Langage | Java 1.8 |
| Framework | Spring 5.0.8, Spring Security 5.0.7, Spring Data JPA 2.0.9 |
| ORM | Hibernate 5.3.4 |
| Base de données | Oracle (JDBC 11.2), pool Apache Commons DBCP 2 |
| Front | JSF 2.2.17, PrimeFaces 6.2 (thème Sentinel), Font Awesome |
| Build | Maven 3.x, Liquibase 3.6 |
| Logging | Log4j 1.2 / SLF4J 1.7 |
| Utilitaires | Apache POI 3.17 (Excel), jcraft.jsch 0.1.55 (SSH/SFTP) |

---

## 2. Entités Métier et Relations

### 2.1 Entités SVFE (Lecture Seule — Système Externe)

*Package : `org.bpc.svfe.entities` — Mappage vers Oracle SVFE externe. Ne jamais écrire dans ces tables.*

| Entité | Clé | Champs principaux | Rôle |
|--------|-----|-------------------|------|
| `CmsAtm` | `Long pid` | institution, terminalId, address, ville, currency1-4 (billets, quantités), statusId | GAB/ATM depuis Select System (4 cassettes) |
| `CmsPos` | `String terminalID` | merchant, ville, quartier, institution | Terminaux POS |
| `CmsTransaction` | `Long id` | network, responseCode, acquirer, issuer, responseDesc, nbre | Transactions avec code succès |
| `CmsFraud` | composite | institution, bin, rule, nbreTransaction, nbreFraude | Alertes fraude par BIN/règle |
| `CmsNetwork` | `String networkCode` | description, statusId | Réseaux monétiques |
| `CmsHost` | `Long pid` | hostname, ipAddress, statusId | Hôtes/interfaces réseau |
| `CmsAccount` | `Long id` | institution, currency, account, balance | Soldes comptes (flotte) |
| `CmsCardLimit` | composite | institution, cardNumber, limit | Cartes hors limites PP |
| `CmsRule` | `ruleId` | ruleDescription, ruleStatus | Règles de détection fraude |
| `SSAtm` | `Long pid` | terminalId, adress, ville, cassette1-4, note, nombreRest, statusId | ATM via Select System |

### 2.2 Entités Monitoring (Lecture/Écriture)

*Package : `org.intelligentsia.monitoring.*` — Entités métier avec champs calculés*

| Entité | Clé | Champs principaux | Rôle |
|--------|-----|-------------------|------|
| `Atm` | `Long pid` | institution, currencyOne/Two, thresholdAmountOne/Two, terminalId, ville | ATM mappé à Institution + 2 devises avec seuils |
| `AtmDetail` | `AtmDetailPK(pid, queryDate)` | 4 cassettes (currency, note, quantity), statusId, exceedLimitOne/Two, champs couleur @Transient | Snapshot d'état ATM par cycle de requête |
| `Host` | `Long pid` | hostname, ipAddress, operatingStatus, processingStatus, queryDate | Snapshot hôte réseau (jamais mis à jour) |
| `Network` | `Long pid` | networkCode, operatingStatus, processingStatus, transactionMode, queryDate | Snapshot statut réseau |
| `TransactionRate` | `(network, acquirer, issuer, queryDate)` | nbreSucces, tauxSucces, nbreEchec, tauxEchec | Taux succès/échec calculés depuis CmsTransaction |
| `TransactionError` | `(network, responseCode, queryDate)` | responseDesc, nbre | Erreurs agrégées par code réponse |
| `FraudRate` | `(institution, bin, queryDate)` | nbreSuspect, nbreFraude | Indicateurs fraude par BIN |
| `FraudDetail` | `(institution, bin, rule, queryDate)` | ruleDesc, nbreSuspect, nbreFraude, description | Fraude détaillée par règle |
| `Flotte` | composite | institution, compte, devise, solde, dernierSolde, deltaSolde1/2, depassement | Solde flotte avec flags dépassement |
| `CarteHorsLimite` | composite | institution, carte, devise, montantLimit, montantActuel | Cartes dépassant limites PP |
| `InstitutionDevise` | `(institutionId, deviseId)` | limitAmountATM, limitAmountCardPP | Seuils par paire institution/devise |
| `TeleCollect` | composite | institution, terminal, dateCollect, statut | Statut télécollecte POS |
| `Pos` | `String id` | institution, merchant, ville, quartier, agent (FK Technico) | Configuration terminal POS |
| `Technico` | `Long id` | name, phone, cards, institution | Technicien support |
| `ExtractionOp` | `Long id` | dateDebut, dateFin, institution, terminals (string CSV) | Opération d'extraction ATM (batch) |
| `Extraction` / `ExtractionPOS` | `Long id` | institution, dateDebut, dateFin, [merchant, ville pour POS] | Extraction TPE/POS (batch) |
| `ExtractTransaction` / `ExtractPOSTransaction` | composite | extraction (FK), transactionDate, amount, codes statut | Enregistrement transaction extrait |

### 2.3 Entités Framework (Sécurité & Configuration)

| Entité | Clé | Champs principaux | Rôle |
|--------|-----|-------------------|------|
| `Utilisateur` | `Long id` | login, password (BCrypt), status (EStatus), role (FK), institution (FK), validity dates | Compte utilisateur système |
| `Role` | `Long id` | code, intitule | Rôle Spring Security |
| `Profil` | `Long id` | code, intitule | Profil groupant des permissions |
| `PasswordHistory` | `Long id` | password (BCrypt), user (FK) | Historique des mots de passe |
| `Institution` | `Long id` | currencyOneId, amountLimitOne, currencyTwoId, amountLimitTwo, institutionID, hostPID | Institution bancaire (sous-classe de Parametre) |
| `Devise` | `Long id` | codeNumerique (ISO 4217), codeAlphabetique, intitule | Définition de devise |
| `Parametre` | `Long id` | typeParametre (FK), code, intitule, fieldOne-fieldSeven | Stockage générique de paramètres |
| `MainMenu` / `MenuItem` / `SubMenu` | `Long id` | code, intitule, action, icon, portal (FK) | Structure de navigation |
| `Portal` | `Long id` | code, searchPortal (FK), detailPortal (FK) | Définition page/vue UI |
| `Label` / `Message` | `Long id` | code, intitule, langCode | Ressources i18n |
| `Fichier` / `Section` / `Zone` / `Colonne` | `Long id` | delimiter, processLevel, sectionName, columnIndex | Configuration parsing de fichiers |

### 2.4 Schéma de Relations (Résumé)

```
Institution (Parametre)
  ├── [1..N] Atm           (INST_ID → Parametre.ID)
  ├── [1..N] Pos            (INST_ID)
  ├── [1..N] Technico       (INST_ID)
  ├── [1..N] ExtractionOp   (INSTITUTION_ID)
  ├── [N..M] Devise         via InstitutionDevise
  └── [1..N] Utilisateur    (INST_ID)

Atm
  └── [1..N] AtmDetail      (composite PK pid + queryDate)

Pos
  └── [0..1] Technico       (TECHNICO_ID nullable)

ExtractionOp / Extraction
  └── [1..N] ExtractTransaction / ExtractPOSTransaction

Utilisateur
  ├── [N..1] Role
  ├── [N..1] Profil
  └── [1..N] PasswordHistory
```

---

## 3. Fonctionnalités Existantes

### 3.1 Dashboard Temps Réel

- Affichage temps réel des données depuis plusieurs repositories (AtmDetail, Host, Network, etc.)
- **Filtrage par institution** : si l'utilisateur a une institution assignée, afficher seulement ses données
- **Rendu couleur dynamique** via méthodes `@Transient` dans les entités (vert/orange/rouge selon seuils)
- **Alertes sonores** via `SoundAlert` (`monitoringAlert.wav`) sur événements critiques
- **Sections affichées :**
  - Hôtes (statut connectivité)
  - Réseaux (connectivité et mode transactionnel)
  - Taux de transactions (succès/échec par réseau)
  - Erreurs de transactions (max 5 par réseau)
  - ATMs fermés (par institution)
  - ATMs hors seuil (cassettes sous limite)
  - Taux de fraude (par BIN)
  - Détails fraude (agrégés par règle)
  - Soldes flotte
  - Cartes hors limites PP

### 3.2 Gestion des Extractions ATM (GAB)

- **CRUD ExtractionOp** (opérations batch) : dateDebut, dateFin, institution, terminaux
- Création d'une nouvelle extraction batch
- Recherche par institution, plage de dates, pattern terminal
- Vue détaillée avec transactions associées
- Classes de recherche avec pagination via `AbstractSearchClass`

### 3.3 Gestion des Extractions TPE/POS

- **CRUD Pos** (terminaux POS) : merchant, ville, quartier, agent technicien
- Extraction de transactions POS/TPE par institution, terminal, plage de dates
- Vue détail par transaction
- Liaison POS ↔ Technicien

### 3.4 Surveillance Fraude

- Suivi des taux de fraude par institution + BIN + date
- Drill-down par règle de fraude
- Recherche et filtrage par institution, BIN, règle, plage de dates

### 3.5 Configuration Système (CRUD complet)

| Module | Entités gérées |
|--------|---------------|
| Gestion des utilisateurs | Utilisateur (login, password BCrypt, rôle, institution, validité) |
| Gestion des rôles/profils | Role, Profil |
| Gestion des institutions | Institution (sous-classe Parametre avec champs flexibles) |
| Gestion des devises | Devise |
| Gestion des paramètres | Parametre, TypeParametre (EParametre enum) |
| Navigation | MainMenu, MenuItem, SubMenu, Portal |
| Internationalisation | Label, Message (langCode) |
| Parsing de fichiers | Fichier → Section → Zone → Colonne |

### 3.6 Système de Validation

- Moteur de validation configurable par base de données (`EntityValidatorEngineImpl`)
- Algorithmes intégrés : `CheckStringIsNotEmpty`, `CheckStringIsEmail`, `CheckFieldIsMin/Max`, `CheckDateIsInFuture/Past`, `CheckFieldIsUnique`
- Configuré via `EntityFieldAlgorithme` + `EntityFieldAlgorithmeParameter`

### 3.7 Tâches de Requêtage Périodique (Timer Tasks)

| Tâche | Intervalle | Tables alimentées |
|-------|-----------|-------------------|
| `HostQueryTask` | 5 min | ST_MN_HOST |
| `NetworkQueryTask` | 5 min | ST_MN_NETWORK |
| `TransactionQueryTask` | 5 min | ST_MN_TRANS_RATE, ST_MN_TRANS_ERR |
| `ATMQueryTask` | 10 min | ST_MN_ATM_DETAIL |
| `FraudQueryTask` | 10 min | ST_MN_FRAUD_RATE, ST_MN_FRAUD_DETAIL |
| `FlotteQueryTask` | 10 min | ST_MN_FLOTTE |
| `CardLimitQueryTask` | 10 min | ST_MN_CARTE_HS_LIMITE |
| `POSTeleCollectQueryTask` | 24 h (23h00) | ST_MN_TELECOLLECT |

---

## 4. Endpoints API

> **Important :** Ce projet est **basé sur JSF, pas REST.** Il n'y a pas d'API REST à proprement parler. Tous les « endpoints » sont des méthodes d'action JSF exposées via des backing beans.

### Pattern d'URL

```
/pages/secure/**          → Requiert ROLE_USER (Spring Security)
/pages/unsecure/**        → Accès libre
/javax.faces.resource/**  → Ressources JSF (CSS, JS, images)
```

### Pages JSF et Backing Beans Associés

| Page JSF | Backing Bean | Actions disponibles |
|----------|-------------|---------------------|
| `/pages/secure/monitoring/dashboard.xhtml` | `DashBoardBean` | `init()`, `refresh()` |
| `/pages/secure/monitoring/extraction/gab/extractionOpPage.xhtml` | `ExtractionOpMainController` | CRUD : `newRecord()`, `update()`, `delete()`, `search()` |
| `/pages/secure/monitoring/extraction/gab/extractionOpDetailPage.xhtml` | `ExtractionOpDetailController` | Vue détail avec transactions |
| `/pages/secure/monitoring/pos/posPage.xhtml` | `PosMainController` | CRUD POS + dropdown institution |
| `/pages/secure/configuration/utilisateur/utilisateurPage.xhtml` | `UtilisateurMainController` | CRUD utilisateurs |
| `/pages/secure/configuration/institution/...` | Controllers institution | CRUD institutions |
| `/pages/secure/configuration/parametre/...` | Controllers paramètre | CRUD paramètres génériques |
| `/pages/unsecure/login.xhtml` | (Spring Security) | Authentification |

### Méthodes d'Action Communes (AbstractMainBean)

```java
searchController.search()          // Déclenche la recherche avec critères
searchController.clearSearch()     // Réinitialise les filtres
newRecord()                        // Initialise formulaire nouveau
update()                           // Charge enregistrement pour édition
setupView()                        // Charge enregistrement en lecture seule
delete()                           // Supprime l'enregistrement sélectionné
save()                             // Persiste l'entité (création ou mise à jour)
```

---

## 5. Schéma de Base de Données

### 5.1 Tables Monitoring (Préfixe `ST_MN_`)

```sql
ST_MN_ATM
  PID (PK), INST_ID (FK), CURRENCY_ONE_ID (FK), TRESHOLD_AMOUNT_ONE,
  CURRENCY_TWO_ID (FK), TRESHOLD_AMOUNT_TWO, TERMINAL_ID, VILLE, ADDRESS

ST_MN_ATM_DETAIL
  PID + QUERY_DATE (PK composite)
  INSTITUTION, TERMINAL_ID
  CURRENCY_1/2/3/4, NOTE_1/2/3/4, NBRE_1/2/3/4
  STATUS_ID, STATUS_DESC, CMD, STATUT_CMD
  CMD_C1/2/3/4, STATUT_CMD_C1/2/3/4
  IMP_RECU, STATUT_IMP_RECU, ALL_OK, STATUS_MESSAGE

ST_MN_HOST
  PID (PK), HOSTNAME, IP_ADDRESS, OPERATING_STATUS, PROCESSING_STATUS, QUERY_DATE

ST_MN_NETWORK
  PID (PK), NETWORK_CODE, DESCRIPTION, OPERATING_STATUS, PROCESSING_STATUS,
  TRANSACTION_MODE, QUERY_DATE

ST_MN_TRANS_RATE
  NETWORK + ACQUIRER + ISSUER + QUERY_DATE (PK composite)
  NBRE_SUCCES, TAUX_SUCCES, NBRE_ECHEC, TAUX_ECHEC

ST_MN_TRANS_ERR
  NETWORK + RESPONSE_CODE + QUERY_DATE (PK composite)
  RESPONSE_DESC, NBRE

ST_MN_FRAUD_RATE
  INSTITUTION + BIN + QUERY_DATE (PK composite)
  NBRE_SUSPECT, NBRE_FRAUDE

ST_MN_FRAUD_DETAIL
  INSTITUTION + BIN + RULE + QUERY_DATE (PK composite)
  RULE_DESC, NBRE_SUSPECT, NBRE_FRAUDE, BINS, DESCRIPTION

ST_MN_FLOTTE
  INSTITUTION + COMPTE + DEVISE + QUERY_DATE (PK composite)
  SOLDE, DERNIER_SOLDE, DELTA_SOLDE_1, DELTA_SOLDE_2,
  DEPASSEMENT_DELTA_1, DEPASSEMENT_DELTA_2

ST_MN_CARTE_HS_LIMITE
  INSTITUTION + CARTE + DEVISE + QUERY_DATE (PK composite)
  MONTANT_LIMIT, MONTANT_ACTUEL, DEPASSEMENT

ST_MN_INST_DEV
  INST_ID + DEV_ID (PK composite)
  LIMIT_WARN_ATM, LIMIT_WARN_CARD_PP

ST_MN_POS
  TERMINAL_ID (PK), INST_ID (FK), MERCHANT, VILLE, QUARTIER, AUTRE_NOM,
  TECHNICO_ID (FK nullable)

ST_MN_TECHNICO
  ID (PK, SEQUENCE), INST_ID (FK), NAME, PHONE, CARDS

ST_MN_EXTRACTION_OP
  ID (PK, SEQUENCE), INSTITUTION_ID (FK), DATE_DEBUT, DATE_FIN, TERMINALS (CSV)

ST_MN_EXTRACTION_TPE
  ID (PK, SEQUENCE), INSTITUTION_ID (FK), DATE_DEBUT, DATE_FIN, MERCHANT, VILLE

ST_MN_TELECOLLECT
  INSTITUTION + TERMINAL + DATE_COLLECT + QUERY_DATE (PK composite)
  STATUT
```

### 5.2 Tables Sécurité (Préfixe `ST_SE_`)

```sql
ST_SE_USER
  ID (PK, SEQUENCE), FIRST_NAME, LAST_NAME, DATE_BIRTH, EMAIL, TELEPHONE, LOGIN (unique),
  PASSWORD (BCrypt, longueur 30 — PROBLÈME), STATUS (Active/Inactive),
  ROLE_ID (FK), PWD_VALIDITY_DATE, ACCOUNT_VALIDITY_DATE, LANGUAGE_ID (FK),
  PROFIL_ID (FK), INST_ID (FK)

ST_SE_ROLE
  ID (PK, SEQUENCE), CODE, LABEL

ST_SE_PROFIL
  ID (PK, SEQUENCE), CODE, LABEL

ST_SE_PWD_HIST
  ID (PK, SEQUENCE), PASSWORD (BCrypt), USER_ID (FK)
```

### 5.3 Tables Framework (Préfixe `ST_BS_`)

```sql
ST_BS_DEVISE
  ID (PK), CODE_ISO_NUM (ex: "952"), CODE_ISO_ALPHA (ex: "XOF"), INTITULE

ST_BS_PARAMETRE              -- Single-table inheritance (Institution y compris)
  ID (PK), TYPE_PARAM_CODE (discriminateur), CODE, LABEL,
  FIELD_ONE, FIELD_TWO, ..., FIELD_SEVEN  (colonnes flexibles — anti-pattern)

ST_BS_MENU, ST_BS_MENU_ITEM, ST_BS_SUB_MENU, ST_BS_PORTAL
  -- Structure de navigation

ST_BS_LABEL, ST_BS_MESSAGE
  -- Ressources i18n : CODE, LABEL, LANG_CODE
```

### 5.4 Contraintes et Index

- **Clés primaires** : majority via SEQUENCE Oracle ; clés composites temporelles pour les entités snapshot
- **Clés étrangères** : déclarées via JPA (`@JoinColumn`) mais pas toutes contraintes en base
- **Index** : **aucun index déclaré sur les clés composites temporelles** (risque de full scan)
- **Pas de contraintes UNIQUE** explicites sur les paires métier (ex: INSTITUTION + TERMINAL pour Pos)
- **Pas de contraintes CHECK** sur les valeurs numériques (montants, taux)

---

## 6. Rôles Utilisateurs

### 6.1 Framework d'Authentification

- **Spring Security 5.0.7** avec BCrypt
- Login via `/pages/unsecure/login.xhtml` → POST → `LoginDetailsService.loadUserByUsername()`
- Session créée après authentification réussie → redirect vers `/pages/secure/index.xhtml`
- Logout via `/logout` → invalidation session → redirect login

### 6.2 Rôles Définis

| Rôle | Accès |
|------|-------|
| `ROLE_USER` | Pages `/pages/secure/**` — consultation et saisie |
| `ROLE_ADMIN` | Pages de configuration, gestion des utilisateurs |

> D'autres rôles peuvent être ajoutés dynamiquement via l'entité `Role` en base.

### 6.3 Multi-Tenancy par Institution

| Condition | Comportement |
|-----------|-------------|
| `user.institution != null` | Filtrage automatique : seules les données de son institution sont visibles |
| `user.institution == null` | Vue globale : toutes institutions affichées (compte administrateur) |

### 6.4 Utilisateurs par Défaut (Problème de Sécurité)

Créés dans `FrameworkInitialization.@PostConstruct` avec des mots de passe en dur dans le code source :

- login : **`Gaetan`**, password : `gaetan`
- login : **`MONG`**, password : `mong`

---

## 7. Points Faibles et Limitations

### 7.1 Sécurité (CRITIQUE)

| # | Problème | Impact |
|---|----------|--------|
| 1 | **Credentials en dur** dans `FrameworkInitialization.java` | Mêmes identifiants déployés partout, visibles dans Git |
| 2 | **CSRF désactivé** : `<sec:csrf disabled="true" />` dans `security.xml` | Vulnérable aux attaques CSRF sur toutes les actions state-mutating |
| 3 | **Session timeout = -1** dans `web.xml` | Sessions ne expirent jamais — session compromise reste valide indéfiniment |
| 4 | **Pas de sécurité méthode** : aucun `@PreAuthorize` sur les services | N'importe quel utilisateur authentifié peut appeler les services admin |
| 5 | **`PasswordHistory.PASSWORD` longueur = 30** | BCrypt hash = 60 chars → troncation → validation échoue |
| 6 | **Pas de journal d'audit** | Impossible de tracer qui a fait quoi |

### 7.2 Logging et Gestion d'Erreurs (CRITIQUE)

- Toutes les 8 tâches Timer utilisent `System.out.println()` et `e.printStackTrace()`
- Les erreurs dans les cycles de requêtage sont **silencieuses** (pas d'alerte, pas de trace durable)
- SLF4J est dans le classpath mais **non utilisé**
- Exemple :
  ```java
  System.out.println("Starting Host Query Task");
  } catch (Exception e) {
      e.printStackTrace(); // Perdu dans la console
  }
  ```

### 7.3 Transactions et Concurrence (CRITIQUE)

- **Aucun `@Transactional`** sur les tâches Timer → exécution hors contexte Spring
- Accès concurrent non synchronisé : `FraudQueryTask` utilise `HashMap` sans synchronisation
- **Risque `LazyInitializationException`** : `AtmDetail.@PrePersist` accède à `atm.getCurrencyOne()` (lazy)
- `enable_lazy_load_no_trans=true` masque les problèmes N+1

### 7.4 Architecture des Timer Tasks

```java
// MonitoringApplication.java @PostConstruct
Timer timer = new Timer();
timer.schedule(new HostQueryTask(hostRepository, ...), 0, period);
// Java Timer — pas de @Scheduled Spring, pas de contexte Spring dans run()
```

- Java `Timer` (pas `@Scheduled`) → pas de lifecycle Spring, pas de retry automatique
- Pas de logique de retry en cas d'échec
- Pas de flags d'activation/désactivation sans redéploiement
- Tous les timers sur un seul thread → un ralentissement retarde tous les autres

### 7.5 Performance

| Problème | Description |
|----------|-------------|
| Pas de pagination | `findAll()` charge toute la table en mémoire, filtrage en Java |
| Pas d'index temporel | Clés composites avec QUERY_DATE sans index → full scan |
| Croissance illimitée | Snapshots accumulés indéfiniment, aucune politique de purge |
| N+1 queries | `enable_lazy_load_no_trans=true` cache le problème sans le résoudre |

Estimation de croissance : 8 snapshots/jour × 365 jours × 5+ tables = **14 000+ lignes/table/an** minimum.

### 7.6 Qualité du Code

| Problème | Exemple |
|----------|---------|
| Typo dans méthode | `findNetwok()` (manque le 'r') |
| Logique de présentation dans les entités | `@Transient getColor()` retourne `"#FF0000"` directement dans l'entité |
| Pas de tests unitaires | Aucun répertoire `src/test/` dans aucun module |
| Valeurs magiques | Indices cassettes codés en dur : `if (cassette == 1) { note1 = ... }` |
| Null checks manquants | `Collections.sort(closedAtms)` sans vérification nullité |
| `buildTraceInformation()` retourne `null` | Méthode non implémentée |
| Commentaires TODO non résolus | Code commenté dans DashBoardBean (disk monitoring) |

### 7.7 Modèle de Données

| Problème | Impact |
|----------|--------|
| **Anti-pattern `fieldOne-fieldSeven`** dans `Parametre` | Schéma non auto-documenté, difficile à maintenir |
| **Terminaux stockés en CSV** dans `ExtractionOp.terminals` | Non normalisé, non indexable, parsing fragile |
| **Pas de colonnes d'audit** | Aucune traçabilité create_at, updated_by, etc. |
| **Pas de soft delete** | Suppressions permanentes, pas d'historique |
| **Dates en `java.util.Date`** | Sensibilité aux fuseaux horaires, pas de `LocalDateTime` JPA 2.1+ |

### 7.8 Fonctionnalités Manquantes

| Fonctionnalité | Statut |
|----------------|--------|
| Notifications temps réel (WebSocket/SSE) | Absent — dashboard rafraîchi manuellement |
| Génération de rapports Excel/PDF | Infrastructure POI présente mais non utilisée |
| Alertes email/SMS | Non implémenté (module `notification/` sous-développé) |
| Export CSV/Excel des résultats | Absent |
| Surveillance disque | Entités présentes (`DiskSpace`, `Machine`, SSH via jsch) mais code commenté |
| Monitoring du monitoring | Pas de health check pour le système lui-même |
| Tests automatisés | Zéro couverture de tests |

### 7.9 Dépendances et Couplage

- **Pas d'anti-corruption layer** : les repositories CMS mappent directement le schéma SVFE externe → toute modification SVFE casse le code
- **Dépendance CloudConvert** déclarée dans le POM mais jamais utilisée
- **Scan de packages trop large** : `org.intelligentsia,org.bpc` → risque que des classes CMS soient instanciées comme beans Spring

---

## 8. Configuration et Déploiement

### 8.1 Propriétés Externes

Fichier requis : `$CATALINA_HOME/conf/monitoring/monitoring.properties`

```properties
MainDBHost=localhost
MainDBPort=1521
MainDBDatabase=monitoring_db
MainDBUsername=Monitoring
MainDBPassword=Monitoring123
MainDBGenerateDDL=false
MainDBShowSQL=false
MainDBConnexionMaxActive=20
```

### 8.2 Séquence de Démarrage

1. Tomcat démarre → Spring Context chargé
2. `FrameworkInitialization.@PostConstruct` → création utilisateurs défaut (mot de passe en dur)
3. `MonitoringInitialization.@PostConstruct` → création modules, menus, portails
4. `MonitoringApplication.@PostConstruct` → lecture EParametre → démarrage des 8 timers

### 8.3 Build

```bash
# Ordre obligatoire des modules
cd framework-parent && mvn clean install
mvn clean install -pl framework-utilities,framework-security,framework-core,framework-jsf,monitoring,notification --also-make

# Artefacts générés
monitoring/target/monitoring.war    # WAR principal
notification/target/notification.war # WAR secondaire
```

---

## 9. Fichiers Clés

| Fichier | Rôle |
|---------|------|
| `monitoring/.../initialization/MonitoringApplication.java` | Démarre les 8 tâches Timer |
| `monitoring/.../configuration/MonitoringConfiguration.java` | Config Spring/Hibernate, DataSource |
| `framework-core/.../initialization/FrameworkInitialization.java` | Création utilisateurs défaut (MOTS DE PASSE EN DUR) |
| `monitoring/src/main/resources/security.xml` | Config Spring Security (CSRF désactivé) |
| `monitoring/src/main/webapp/WEB-INF/web.xml` | Servlet, filtres, timeout session (-1) |
| `framework-utilities/.../entities/Parametre.java` | Anti-pattern fieldOne-fieldSeven |
| `framework-security/.../encryption/Encryption.java` | Encodeur BCrypt |
| `framework-utilities/.../monitoring/EParametre.java` | Enum des paramètres configurables |
| `monitoring/.../controllers/DashBoardBean.java` | Logique d'affichage tableau de bord |

---

## 10. Résumé des Priorités de Refactorisation

### Priorité Haute (Sécurité & Stabilité)

1. Externaliser les credentials par défaut (variables d'environnement ou config chiffrée)
2. Réactiver CSRF (`disabled=false` + tokens dans les formulaires JSF)
3. Définir un timeout de session raisonnable (30-60 minutes)
4. Implémenter le logging SLF4J dans les tâches Timer (remplacer `System.out`/`e.printStackTrace`)
5. Corriger la longueur de colonne `PasswordHistory.PASSWORD` (60+ chars pour BCrypt)

### Priorité Moyenne (Qualité & Maintenabilité)

6. Migrer les `Java Timer` vers `@Scheduled` Spring avec `TransactionTemplate`
7. Ajouter une logique de retry avec backoff exponentiel
8. Ajouter des index sur les clés composites temporelles (QUERY_DATE)
9. Implémenter un journal d'audit (created_at, updated_at, created_by, updated_by)
10. Déplacer la logique de présentation (`@Transient getColor()`) vers la couche vue (JSF converters)

### Priorité Basse (Fonctionnalités Complémentaires)

11. Implémenter WebSocket pour rafraîchissement automatique du tableau de bord
12. Compléter le module de génération de rapports (Apache POI déjà en place)
13. Implémenter le système d'alertes email/SMS
14. Activer et finaliser la surveillance disque (code déjà partiellement là)
15. Ajouter tests unitaires et d'intégration (couverture cible > 80%)
16. Implémenter une politique de rétention des données (archivage/suppression après N jours)
