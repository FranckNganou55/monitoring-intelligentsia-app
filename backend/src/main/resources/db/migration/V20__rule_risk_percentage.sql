ALTER TABLE monitoring_rules
    ADD COLUMN risk_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE monitoring_rules
    ADD CONSTRAINT chk_monitoring_rules_risk_percentage
        CHECK (risk_percentage >= 0.00 AND risk_percentage <= 100.00);

-- Legacy-compatible administrator permissions. ROLE_READ is required by the
-- user-management role selector; admins receive every permission seeded by V19.
INSERT INTO permissions (code, name, resource, action)
VALUES ('ROLE_READ', 'Consulter les rôles', 'ROLE', 'READ')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

UPDATE users u
SET scope = 'ADMIN', institution_id = NULL
WHERE EXISTS (
    SELECT 1
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = u.id
      AND r.code = 'ROLE_ADMIN'
);
