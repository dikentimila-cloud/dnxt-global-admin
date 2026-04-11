-- =============================================================================
-- Support Access permission — only Super Admin can "Login As" into tenants
-- =============================================================================

INSERT INTO admin_permission (permission_id, code, module, action, description)
VALUES ('perm-0016', 'SUPPORT_ACCESS', 'TENANT', 'SUPPORT',
        'Access tenant environments via Login-As (generates temporary support session)');

-- Grant only to Super Admin
INSERT INTO admin_role_permission (role_id, permission_id)
VALUES ('role-superadmin-0001', 'perm-0016');
