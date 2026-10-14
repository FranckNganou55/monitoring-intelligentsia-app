# Interfaces GAB, transactions et source des données

## DOWN et SIGNOFF

Administration > Paramètres > Alertes contient deux cartes indépendantes : seuil de 1 à 20 relevés consécutifs et fréquence de 1 à 15 minutes. Chaque carte possède son bouton de validation. Les changements sont enregistrés en base et appliqués au prochain cycle.

La migration V33 copie le seuil partagé et la fréquence existante vers deux réglages séparés. V1 à V32 restent intactes. Le collecteur commun tourne assez fréquemment pour respecter la plus courte des fréquences; seuls les relevés espacés selon la règle correspondante comptent. Un changement de statut démarre une autre série; UP et SIGNON réinitialisent immédiatement l’incident.

Endpoint administrateur : `PUT /api/settings/atm-interface/DOWN` ou `/SIGNOFF`, corps `{"samples": 3, "intervalMs": 300000}`. Les limites sont contrôlées côté serveur.

## Transactions

Le menu Transactions est placé sous Réseaux. Il donne accès aux volumes et taux de succès, aux codes de réponse en échec et aux transactions liées aux détections. Cette dernière vue affiche les champs déjà présents en base : référence, type, montant, devise, commerçant, réponse, institution, règle et risque. Une transaction peut apparaître plusieurs fois si plusieurs règles l’ont détectée. Elle ne prétend pas constituer le journal exhaustif d’Oracle.

Les types de transaction sont lus dans `fraud_detections.transaction_type`, sans catalogue inventé. Les filtres de type, recherche et dates s’appliquent côté backend avec pagination et isolation par institution. Le code 00 représente un succès dans le backend existant. Les agrégats réseau n’ayant pas de clé institutionnelle restent accessibles uniquement dans le contexte Toutes les institutions.

Endpoints : `GET /api/transactions/types`, `GET /api/transactions/operations` (page, size, institutionId, from, until, type, search), plus les endpoints rates/errors existants.

## Données de test et coupure

Les GABs, institutions, observations, cassettes de test et alertes sont des enregistrements de la base PostgreSQL actuelle. Les fichiers JSON et SQL sous `backend/scripts/test-data` sont uniquement des outils manuels de préparation; le backend et le frontend ne les chargent pas au démarrage. Les migrations ne créent aucune institution, GAB ou alerte de démonstration. Les règles, rôles, permissions et le compte administrateur initial sont de la configuration.

Une perte de connexion à la base applicative produit HTTP 503 / DATABASE_UNAVAILABLE, y compris pendant l’authentification JWT. Le frontend masque les pages métier, vide son cache de requêtes, la sélection d’institution et les notifications, ferme le signal d’alerte et affiche zéro pour les compteurs dans un écran Base de données indisponible. Un contrôle périodique (15 secondes, délai réseau 5 secondes) et un bouton permettent de détecter le retour de la connexion. Ces zéros représentent une absence de données disponibles, pas une suppression en base.

Les données enregistrées restent dans la base déconnectée. Une base applicative vide donne des listes vides et des compteurs nuls; aucune donnée de démonstration n’est reconstituée. La reconnexion recharge les données depuis le backend.

L’architecture actuelle utilise PostgreSQL comme base applicative et Oracle SVFE comme source en lecture seule. Activer Oracle sans changer la base applicative conserverait donc les anciens tests; le déploiement sur une base applicative propre les laisse dans la base de test. Un remplacement complet de PostgreSQL par Oracle nécessiterait une adaptation des migrations et requêtes PostgreSQL existantes.
