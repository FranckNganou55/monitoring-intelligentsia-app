-- V12 : Permissions manquantes pour Network, Host et Transaction
-- Ces 3 permissions étaient absentes de V1 mais requises par les contrôleurs.

INSERT INTO permissions (code, name, resource, action) VALUES
    ('NETWORK_READ',     'Consulter les réseaux',        'NETWORK',     'READ'),
    ('HOST_READ',        'Consulter les hôtes',          'HOST',        'READ'),
    ('TRANSACTION_READ', 'Consulter les transactions',   'TRANSACTION', 'READ');

-- Accorder les 3 nouvelles permissions à ROLE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ROLE_ADMIN'
  AND p.code IN ('NETWORK_READ', 'HOST_READ', 'TRANSACTION_READ');

-- Accorder en lecture aux MANAGER et OPERATOR (données de monitoring)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('ROLE_MANAGER', 'ROLE_OPERATOR')
  AND p.code IN ('NETWORK_READ', 'HOST_READ', 'TRANSACTION_READ');
