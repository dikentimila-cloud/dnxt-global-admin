-- =============================================================================
-- DnXT Global Admin Portal — Seed Data
-- Roles, permissions, default config, initial super-admin user
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------------
INSERT INTO admin_role (role_id, role_name, role_label, description, is_system) VALUES
('role-superadmin-0001', 'SUPER_ADMIN', 'Super Admin', 'Full platform access — can manage tenants, users, config, and other admins', TRUE),
('role-admin-0001',      'ADMIN',       'Admin',       'Can manage tenants and view platform health', TRUE),
('role-viewer-0001',     'VIEWER',      'Viewer',      'Read-only access to tenant and platform data', TRUE);

-- ---------------------------------------------------------------------------
-- Permissions
-- ---------------------------------------------------------------------------
INSERT INTO admin_permission (permission_id, code, module, action, description) VALUES
-- Tenant management
('perm-0001', 'TENANT_VIEW',       'TENANT', 'VIEW',       'View tenant list and details'),
('perm-0002', 'TENANT_CREATE',     'TENANT', 'CREATE',     'Create new tenants'),
('perm-0003', 'TENANT_EDIT',       'TENANT', 'EDIT',       'Edit tenant details and licensing'),
('perm-0004', 'TENANT_DEACTIVATE', 'TENANT', 'DEACTIVATE', 'Deactivate tenants'),
('perm-0005', 'TENANT_ONBOARD',    'TENANT', 'ONBOARD',    'Full tenant onboarding (create + provision + welcome email)'),
-- Module licensing
('perm-0006', 'MODULE_VIEW',       'MODULE', 'VIEW',       'View module assignments'),
('perm-0007', 'MODULE_MANAGE',     'MODULE', 'MANAGE',     'Enable/disable modules per tenant'),
-- User management (admin portal users)
('perm-0008', 'USER_VIEW',         'USER',   'VIEW',       'View admin portal users'),
('perm-0009', 'USER_CREATE',       'USER',   'CREATE',     'Create new admin portal users'),
('perm-0010', 'USER_EDIT',         'USER',   'EDIT',       'Edit admin portal users'),
('perm-0011', 'USER_DEACTIVATE',   'USER',   'DEACTIVATE', 'Deactivate admin portal users'),
-- Platform config
('perm-0012', 'CONFIG_VIEW',       'CONFIG', 'VIEW',       'View platform configuration'),
('perm-0013', 'CONFIG_EDIT',       'CONFIG', 'EDIT',       'Edit platform configuration'),
-- Dashboard & health
('perm-0014', 'DASHBOARD_VIEW',    'DASHBOARD', 'VIEW',    'View platform dashboard and health'),
-- Audit
('perm-0015', 'AUDIT_VIEW',        'AUDIT',  'VIEW',       'View audit logs');

-- ---------------------------------------------------------------------------
-- Role ↔ Permission Assignments
-- ---------------------------------------------------------------------------
-- Super Admin: everything
INSERT INTO admin_role_permission (role_id, permission_id) VALUES
('role-superadmin-0001', 'perm-0001'),
('role-superadmin-0001', 'perm-0002'),
('role-superadmin-0001', 'perm-0003'),
('role-superadmin-0001', 'perm-0004'),
('role-superadmin-0001', 'perm-0005'),
('role-superadmin-0001', 'perm-0006'),
('role-superadmin-0001', 'perm-0007'),
('role-superadmin-0001', 'perm-0008'),
('role-superadmin-0001', 'perm-0009'),
('role-superadmin-0001', 'perm-0010'),
('role-superadmin-0001', 'perm-0011'),
('role-superadmin-0001', 'perm-0012'),
('role-superadmin-0001', 'perm-0013'),
('role-superadmin-0001', 'perm-0014'),
('role-superadmin-0001', 'perm-0015');

-- Admin: tenant + module + dashboard (no user mgmt, no config edit)
INSERT INTO admin_role_permission (role_id, permission_id) VALUES
('role-admin-0001', 'perm-0001'),
('role-admin-0001', 'perm-0002'),
('role-admin-0001', 'perm-0003'),
('role-admin-0001', 'perm-0005'),
('role-admin-0001', 'perm-0006'),
('role-admin-0001', 'perm-0007'),
('role-admin-0001', 'perm-0012'),
('role-admin-0001', 'perm-0014');

-- Viewer: read-only everything
INSERT INTO admin_role_permission (role_id, permission_id) VALUES
('role-viewer-0001', 'perm-0001'),
('role-viewer-0001', 'perm-0006'),
('role-viewer-0001', 'perm-0008'),
('role-viewer-0001', 'perm-0012'),
('role-viewer-0001', 'perm-0014'),
('role-viewer-0001', 'perm-0015');

-- ---------------------------------------------------------------------------
-- Platform Configuration Defaults
-- ---------------------------------------------------------------------------
INSERT INTO platform_config (config_key, config_value, category, description, is_secret) VALUES
-- Security
('security.allowed_email_domains',   'dnxtsolutions.com', 'SECURITY', 'Comma-separated email domains allowed to create accounts', FALSE),
('security.allowed_emails',          '',                  'SECURITY', 'Comma-separated individual emails allowed (overrides domain check)', FALSE),
('security.max_failed_attempts',     '5',                 'SECURITY', 'Max failed login attempts before lockout', FALSE),
('security.lockout_duration_minutes','30',                'SECURITY', 'Account lockout duration in minutes', FALSE),
('security.password_min_length',     '12',                'SECURITY', 'Minimum password length', FALSE),
-- General
('platform.name',                    'DnXT Platform',     'GENERAL',  'Platform display name', FALSE),
('platform.support_email',           'support@dnxtsolutions.com', 'GENERAL', 'Support email address', FALSE),
-- Mail
('mail.smtp.host',                   '',                  'MAIL',     'SMTP server hostname', FALSE),
('mail.smtp.port',                   '587',               'MAIL',     'SMTP server port', FALSE),
('mail.from.address',                'noreply@dnxtsolutions.com', 'MAIL', 'From address for system emails', FALSE),
('mail.from.name',                   'DnXT Platform',     'MAIL',     'From display name', FALSE);

-- ---------------------------------------------------------------------------
-- Default Super Admin User (password: ChangeMe123! — must be changed on first login)
-- BCrypt hash of 'ChangeMe123!' with strength 10
-- ---------------------------------------------------------------------------
INSERT INTO admin_user (user_id, username, email, password_hash, first_name, last_name, role_id, is_active, must_change_password) VALUES
('user-superadmin-0001', 'superadmin', 'admin@dnxtsolutions.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 'Platform', 'Admin', 'role-superadmin-0001', TRUE, TRUE);

-- ---------------------------------------------------------------------------
-- Available Modules (the products DnXT offers)
-- These are the module_name values used in tenant_module table
-- ---------------------------------------------------------------------------
-- Module names: 'Operations', 'Reviewer', 'Publisher', 'EDMS', 'Planner'
-- Phase 1: Only 'Operations' is actively managed
-- Phase 2: All modules become manageable
