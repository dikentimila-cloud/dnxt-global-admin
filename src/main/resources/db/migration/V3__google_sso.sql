-- =============================================================================
-- Google Workspace SSO support
-- =============================================================================

ALTER TABLE admin_user ADD COLUMN google_id VARCHAR(255);
ALTER TABLE admin_user ADD COLUMN avatar_url VARCHAR(500);
ALTER TABLE admin_user ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL';

-- auth_provider: 'LOCAL' = email/password, 'GOOGLE' = Google Workspace SSO
-- google_id: Google's unique subject identifier (sub claim from ID token)

CREATE INDEX idx_admin_user_google_id ON admin_user(google_id);

-- Make password_hash nullable — Google SSO users don't have a local password
ALTER TABLE admin_user ALTER COLUMN password_hash DROP NOT NULL;
