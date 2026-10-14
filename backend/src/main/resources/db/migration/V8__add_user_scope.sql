-- V8 : Scope multi-tenant sur les utilisateurs
-- INSTITUTION = voit uniquement son institution
-- GLOBAL      = voit toutes les institutions, sans droits d'administration
-- ADMIN       = accès total à la plateforme

ALTER TABLE users
    ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'INSTITUTION'
        CONSTRAINT chk_user_scope CHECK (scope IN ('INSTITUTION', 'GLOBAL', 'ADMIN'));

-- Le compte admin système passe en scope ADMIN
UPDATE users SET scope = 'ADMIN' WHERE login = 'admin';
