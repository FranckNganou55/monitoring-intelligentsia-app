ALTER TABLE monitoring_rules
    DROP CONSTRAINT IF EXISTS monitoring_rules_code_key;

CREATE INDEX IF NOT EXISTS idx_monitoring_rules_code
    ON monitoring_rules(code);

-- Each account uses one built-in role; the role determines its institution scope.
WITH canonical_roles AS (
    SELECT u.id AS user_id,
           CASE
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_ADMIN'
               ) THEN 'ROLE_ADMIN'
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_MANAGER'
               ) THEN 'ROLE_MANAGER'
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_OPERATOR'
               ) THEN 'ROLE_OPERATOR'
               WHEN u.scope = 'ADMIN' THEN 'ROLE_ADMIN'
               WHEN u.scope = 'GLOBAL' THEN 'ROLE_MANAGER'
               ELSE 'ROLE_OPERATOR'
           END AS role_code
    FROM users u
)
INSERT INTO user_roles (user_id, role_id)
SELECT c.user_id, r.id
FROM canonical_roles c
JOIN roles r ON r.code = c.role_code
ON CONFLICT (user_id, role_id) DO NOTHING;

WITH canonical_roles AS (
    SELECT u.id AS user_id,
           CASE
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_ADMIN'
               ) THEN 'ROLE_ADMIN'
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_MANAGER'
               ) THEN 'ROLE_MANAGER'
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_OPERATOR'
               ) THEN 'ROLE_OPERATOR'
               WHEN u.scope = 'ADMIN' THEN 'ROLE_ADMIN'
               WHEN u.scope = 'GLOBAL' THEN 'ROLE_MANAGER'
               ELSE 'ROLE_OPERATOR'
           END AS role_code
    FROM users u
)
DELETE FROM user_roles ur
USING canonical_roles c, roles r
WHERE ur.user_id = c.user_id
  AND r.id = ur.role_id
  AND r.code <> c.role_code;

WITH canonical_roles AS (
    SELECT u.id AS user_id,
           CASE
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_ADMIN'
               ) THEN 'ADMIN'
               WHEN EXISTS (
                   SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id AND r.code = 'ROLE_MANAGER'
               ) THEN 'GLOBAL'
               ELSE 'INSTITUTION'
           END AS scope
    FROM users u
)
UPDATE users u
SET scope = c.scope,
    institution_id = CASE WHEN c.scope = 'INSTITUTION' THEN u.institution_id ELSE NULL END
FROM canonical_roles c
WHERE c.user_id = u.id;

-- Restore all dashboard/monitoring grants and enable alert handling for
-- managers and operators. Platform settings remain admin-only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ROLE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'DASHBOARD_READ', 'ATM_READ', 'POS_READ', 'FRAUD_READ', 'FLEET_READ',
    'EXTRACTION_READ', 'EXTRACTION_WRITE', 'NETWORK_READ', 'HOST_READ',
    'TRANSACTION_READ', 'MONITORING_ALERT_VIEW', 'MONITORING_ALERT_ACKNOWLEDGE',
    'MONITORING_ALERT_ASSIGN', 'MONITORING_ALERT_PROCESS',
    'MONITORING_ALERT_RESOLVE', 'MONITORING_ALERT_ARCHIVE'
)
WHERE r.code IN ('ROLE_MANAGER', 'ROLE_OPERATOR')
ON CONFLICT (role_id, permission_id) DO NOTHING;
