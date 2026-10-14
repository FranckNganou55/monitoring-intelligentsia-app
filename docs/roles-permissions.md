# Gestion des rôles et des permissions

La page Administration > Rôles permet à ROLE_ADMIN de créer un rôle (code ROLE_ unique), sélectionner ses permissions dans le catalogue existant et modifier les permissions des autres rôles. Le rôle Administrateur est protégé. Les modifications sont transactionnelles et auditées. Aucun jeu de rôles fictif n’est ajouté au démarrage.

Les rôles personnalisés sont disponibles dans Administration > Utilisateurs et conservent leurs permissions lorsqu’ils sont attribués. Ils utilisent le périmètre institutionnel ; donner une permission ne donne pas accès aux données d’une autre institution. Les droits de plusieurs rôles se cumulent côté serveur.

API : GET /api/roles, GET /api/roles/permissions (administrateur), POST /api/roles {code,name,description,permissions}, PUT /api/roles/{id}/permissions {permissions}. Le catalogue contient uniquement les permissions existantes en base. Un ensemble vide retire toutes les permissions du rôle. Codes inconnus, codes de rôle invalides et doublons sont refusés.

Le serveur relit les droits en base à chaque requête authentifiée, même avec un ancien JWT. Après validation de la transaction, un événement SSE permissions-changed entraîne la relecture de /api/users/me. Toutes les sessions SSE d’un utilisateur sont prises en charge. Menus et accès aux pages réagissent au nouveau profil ; les données en cache sont réinitialisées. La page ouverte devient inaccessible si son droit de consultation est retiré. Une relecture toutes les 15 secondes, au retour sur la fenêtre et à la reconnexion SSE couvre les interruptions de la connexion temps réel.

Les changements de permissions persistent dans roles/role_permissions. Aucune modification des migrations déjà appliquées n’est nécessaire.
