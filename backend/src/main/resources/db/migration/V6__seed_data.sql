-- V6 : Données initiales — compte administrateur
-- Mot de passe : Admin@1234 (BCrypt, à changer au premier login)

INSERT INTO users (login, email, password_hash, first_name, last_name, status, language_code)
VALUES (
    'admin',
    'admin@monitoring.local',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4tbQJ1XYVC',
    'Administrateur',
    'Système',
    'ACTIVE',
    'fr'
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.login = 'admin' AND r.code = 'ROLE_ADMIN';
