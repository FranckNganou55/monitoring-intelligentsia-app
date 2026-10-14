# Ancienne version et fonctionnement sans base métier

## Analyse du ZIP

Le ZIP contient les migrations V1 à V12, un frontend dont AppLayout conserve l’affichage des pages et deux connexions. application.yml utilise PostgreSQL (monitoring_dev par défaut). SvfeDataSourceConfig active la connexion Oracle lorsque SVFE_ENABLED est vrai. Les tables Oracle ne sont pas créées par Hibernate et un schéma Oracle peut être précisé.

Les rôles et permissions étaient stockés dans les tables créées par V1 et alimentées par V6. Les paramètres email étaient dans app_settings, créé par V10. Le frontend demandait les rôles à une API. Ces éléments n’étaient donc pas indépendants de PostgreSQL dans le ZIP. Les améliorations réalisées après cette version sont conservées.

La précédente déconnexion arrêtait le backend et le nouvel écran d’indisponibilité remplaçait Outlet. Toutes les pages étaient masquées, alors que seules les données métier devaient disparaître.

## Correction

Le mode local-config conserve rôles, permissions, règles, paramètres, comptes avec mots de passe hachés et secret SMTP déjà chiffré dans backend/local-config/application-state.json. Le dossier est privé et ignoré par Git. Aucun jeton de session n’a été exporté. Le fichier est la source persistante ; H2, uniquement en mémoire, sert d’adaptateur aux repositories existants et ne produit aucun fichier de base de données.

Aucune institution, aucun GAB, aucune alerte, aucune fraude et aucun relevé de test n’a été copié. Ces données restent dans l’ancienne base PostgreSQL. Le backend local ne se connecte pas à PostgreSQL et Oracle n’est pas activé dans ce mode déconnecté.

Le backend reste actif pour les comptes, règles, rôles et paramètres. Les API métier renvoient des listes vides et des compteurs zéro. Aucun pourcentage de disponibilité n’est inventé. Les écritures et exports nécessitant des données métier sont suspendus, avec maintien des contrôles de permissions. Les collectes et notifications automatiques sont suspendues. Les réglages d’alertes et la construction du service SMTP restent disponibles sans alertes reçues.

Les pages, menus, filtres et tableaux restent visibles selon les permissions. Un bandeau indique la déconnexion. La page Règles contient son catalogue avec zéro alerte. Un changement de backend réinitialise les compteurs et recharge les règles.

Les modifications de configuration sont écrites dans le fichier avant la réponse de succès, par remplacement atomique. Les droits sont relus à chaque requête et actualisés via SSE. Les jetons de rafraîchissement restent seulement en mémoire ; après redémarrage, une reconnexion peut être nécessaire à l’expiration du jeton d’accès.

## Démarrage actuel

Exécuter dans PowerShell :

    .\backend\run-local.ps1

Le script démarre le profil local-config sur 8089 et exige le fichier privé. Le frontend reste sur 5173. Les profils et migrations du démarrage historique PostgreSQL n’ont pas été modifiés ; le mode local est activé explicitement.

## Connexion Oracle ultérieure

Les paramètres du connecteur initial sont SVFE_ENABLED, SVFE_DB_URL, SVFE_DB_USER, SVFE_DB_PASS et éventuellement SVFE_DB_SCHEMA. Les requêtes existantes importent les données via SvfeGateway. La nouvelle connexion Oracle n’a pas été activée ni testée dans cette intervention. Son branchement devra conserver le stockage local de configuration et utiliser les institutions et données réellement présentes dans la base cible. Le profil historique PostgreSQL conserve son ancienne architecture et n’utilise pas automatiquement ce fichier local.

## Vérifications

Tests d’intégration : accès à la configuration sans PostgreSQL, connexion normale, renouvellement de jeton, droits protégés, listes métier vides, compteurs zéro, refus des écritures métier et sauvegarde des permissions dans le fichier. Tests supplémentaires : relecture après recréation de la mémoire, absence de données métier et de sessions dans le fichier, hachages des comptes conservés et secret SMTP chiffré utilisable. Aucun email réel n’a été envoyé durant ces vérifications.
