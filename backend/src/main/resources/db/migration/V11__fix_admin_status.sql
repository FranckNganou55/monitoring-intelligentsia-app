-- V11 : Remise en état du compte administrateur système
-- Réinitialise le statut à ACTIVE et s'assure que le scope est ADMIN

UPDATE users
SET status = 'ACTIVE',
    scope  = 'ADMIN'
WHERE login = 'admin';
