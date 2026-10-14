# Audit des migrations V13 à V30 — 12 octobre 2026

## Résultat

Les scripts s’exécutent sur PostgreSQL 16.15, aussi bien sur une installation neuve V1–V30 que sur un schéma V12 contenant des données anciennes. Les tests Flyway valident la chaîne et les sommes de contrôle de la base locale déjà à V30. Aucune migration n’a été modifiée pour cet audit et aucune V31 n’a été ajoutée.

Cela valide la compatibilité **technique avec le schéma fourni par V1–V12**, pas l’absence de changement fonctionnel ni la conformité d’une base de production non connectée. V21 et V28 demandent un examen des données existantes avant mise à jour.

## Deux bases distinctes

- PostgreSQL (`spring.datasource`, unité JPA `primary`) contient les utilisateurs, institutions, GABs, paramètres, historiques et alertes. Flyway V1–V30 s’applique à cette base.
- Oracle SVFE (`app.svfe.datasource`, unité JPA `svfe`) est une source externe. Sa configuration Hibernate utilise `hbm2ddl.auto=none`. Les migrations PostgreSQL ne créent ni ne modifient les tables Oracle.
- Les anciennes colonnes `institutions.svfe_id` et `atms.svfe_pid` restent présentes, avec leurs types et leurs données. Les historiques `atm_snapshots` restent conservés.
- La lecture des nouvelles interfaces utilise `INTERFACES`, `INTERFACE_STATUS`, les champs `INT_*`, `IST_CODE` et `IST_LABE`; V25 conserve ces valeurs dans des tables PostgreSQL distinctes. Le statut est le code `INT_STAT`, son libellé vient de `IST_LABE`.
- La requête JDBC des interfaces utilise des noms Oracle sans préfixe : ils doivent être accessibles dans le schéma de session ou par synonymes. Le paramètre de schéma Hibernate ne préfixe pas automatiquement cette requête JDBC.

La disponibilité des colonnes, les droits SELECT, les synonymes, la correspondance des identifiants GAB/interface et le filtre `INT_BAN_CODE=20` doivent encore être vérifiés contre la véritable source Oracle au moment de sa reconnexion. Aucun accès Oracle réel n’a été effectué lors de cet audit.

## Examen migration par migration

| Version | Évolution | Compatibilité et réserve |
|---|---|---|
| V13 | `monitoring_rules` | BIGSERIAL/BIGINT, types PostgreSQL et références vers les utilisateurs existants. Codes initialement uniques. |
| V14 | Alertes et événements | Références aux règles, institutions, GABs et utilisateurs. La suppression d’une règle utilisée est interdite par `ON DELETE RESTRICT`. JSONB cohérent avec V9. |
| V15 | États GAB/réseau et fenêtres de contrôle | Tables nouvelles, références vers les objets existants, sans remplacement des snapshots V3. |
| V16 | Détections de fraude | Table nouvelle, clés compatibles avec règles/institutions; montants NUMERIC. |
| V17 | Notifications d’alerte | Ajoute les colonnes aux notifications V5 et paramètres au groupe NOTIFICATIONS autorisé par V10. |
| V18 | Rapports journaliers et Word | Remplace la contrainte de formats V9 par CSV/EXCEL/PDF/WORD. Les anciens formats restent autorisés. |
| V19 | Permissions monitoring | Ajoute des permissions. L’affectation utilise `roles.name='ROLE_ADMIN'`, alors que V1 utilise ce texte dans `roles.code`; V20 rétablit les permissions admin via le code. Ne pas s’arrêter à V19 en pensant les droits attribués. |
| V20 | Pourcentages de risque et droits admin | Colonne NUMERIC bornée 0–100; complète les droits admin. Change le scope des utilisateurs admin et supprime leur institution associée. |
| V21 | Variantes de règles et rôles canoniques | Supprime l’unicité des codes de règles, autorisant leurs variantes. **Ramène chaque utilisateur à un rôle standard unique**, supprime ses autres affectations et normalise son scope/institution. Les rôles personnalisés peuvent perdre leurs utilisateurs. |
| V22 | Catalogue des règles importées | Ajout de variantes distinctes, compatible avec V21. Ne restaure pas l’unicité des codes. |
| V23 | Ordre des variantes | Ajout de `source_order` et normalisation de libellés. Les identifiants de règles restent les références d’historique. |
| V24 | Description des hôtes | Ajoute une colonne facultative à `host_snapshots`; ne remplace pas les données existantes. |
| V25 | Interfaces GAB | Tables PostgreSQL nouvelles; champs sources majoritairement TEXT, dates de collecte TIMESTAMPTZ. Modifie la cadence ATM_QUERY à 180000 ms. Ajoute l’ancienne règle combinée, remplacée au démarrage par DOWN et SIGNOFF. |
| V26 | Ordre de l’ancienne règle | Mise à jour de classement, sans altération structurelle. |
| V27 | Mail institution et indicatifs pays | Ajoute colonnes et référentiel; mail institution désactivé par défaut. |
| V28 | Codes pays numériques et mails par risque | **Supprime `institutions.country_code` et `country_calling_codes` avec CASCADE**, sans conversion ni copie des anciennes valeurs. Ajoute `country_numeric_code`, initialement NULL; conserve `alert_email`. Il faut une correspondance métier, un indicatif téléphonique n’étant pas un identifiant pays interne. |
| V29 | Nombre de relevés d’alerte | Paramètre ajouté sans écraser une valeur existante; groupe CRON compatible avec V10. |
| V30 | Cadences et lecture des notifications | Étend la contrainte V10 avec ALERTES; conserve les groupes précédents. Insère les tâches absentes, sans écraser les tâches présentes. Ajoute `read_at` et interprète les anciennes dates comme UTC. Vérifier cette convention si l’ancienne base stockait des heures locales. |

## Vérifications exécutées

`MigrationCompatibilityTest` utilise uniquement des schémas temporaires `codex_audit_<UUID>` dans `monitoring_test`, supprimés en fin de test :

1. Création neuve jusqu’à V30 puis `Flyway.validate()`.
2. Création V12 et insertion d’une institution, d’un GAB, d’un relevé, d’un utilisateur avec plusieurs rôles dont un personnalisé, et modification d’un paramètre SMTP.
3. Passage à V27, attribution d’un ancien indicatif pays et d’une adresse mail institutionnelle.
4. Passage à V30 puis validation.
5. Comparaison de toutes les colonnes V12 : présence, type SQL et longueur conservés. Contrôle des identifiants SVFE, du relevé et du paramètre SMTP.
6. Confirmation explicite des effets V21 (un rôle canonique restant) et V28 (ancien indicatif supprimé, nouveau code NULL, adresse mail conservée).
7. Validation du schéma local V30 et tests des endpoints réels avec Oracle désactivé.

Commande depuis `backend` : `mvn test -Dmonitoring.dbTests=true -Dmonitoring.migrationAudit=true`.

## Préparation de la reconnexion

1. Identifier la base PostgreSQL de l’application et la source Oracle SVFE; ne pas pointer Flyway vers Oracle.
2. Sauvegarder PostgreSQL et exporter les affectations de rôles, scopes/institutions, anciennes valeurs pays éventuelles et paramètres de planification avant migration.
3. Vérifier la table `flyway_schema_history` et les sommes de contrôle contre les scripts réellement déployés auparavant. La base distante n’ayant pas été inspectée, cette égalité reste à établir. Ne pas utiliser `repair` pour masquer une différence sans examen.
4. Rejouer la mise à jour sur une restauration de cette base et décider de la correspondance des pays et du traitement des rôles personnalisés avant application réelle. Ne pas modifier rétroactivement les migrations déjà appliquées.
5. Reconnecter Oracle avec les anciens identifiants de source et contrôler les requêtes en lecture sur de vrais terminaux/interfaces. Les noms exacts des colonnes et les valeurs réseau/GAB doivent être validés dans cette source.

## Règle combinée supprimée

À la demande explicite du 12 octobre 2026, `RULE_ATM_INTERFACE_UNAVAILABLE_5X` a été physiquement supprimée de la base locale, avec ses dix alertes et leur historique de suivi. La règle de test `TEST_FRAUD_ALERT` et ses trois alertes ont également été supprimées. Les notifications correspondantes ont été retirées. Les règles DOWN et SIGNOFF restent actives. La suppression a été effectuée dans une transaction limitée aux identifiants et codes vérifiés de `monitoring_test`, sans changement des migrations.

Le démarrage retire désormais une ancienne règle combinée sans références après création de ses remplacements. Il ne purge pas automatiquement les historiques des autres bases : une telle suppression reste une opération explicite.

## Droit de lecture des rôles

Les trois rôles standards étaient présents dans la base locale, mais la permission `ROLE_READ` et son affectation admin manquaient dans les données actuelles malgré une chaîne Flyway à V30. La validation des scripts ne garantit pas que leurs données d’initialisation n’ont pas été modifiées par la suite. Un initialiseur idempotent rétablit cette permission et l’attribue au rôle `ROLE_ADMIN`, sans élargir les droits des autres rôles ni modifier une migration appliquée. La page distingue maintenant un refus d’accès ou une erreur serveur d’une liste réellement vide.
