-- =============================================================================
-- Fix: Clear SSO redirect URI so env var takes precedence in production.
-- V7 seeded localhost default which overrides the production env var.
-- =============================================================================
UPDATE platform_config SET config_value = '' WHERE config_key = 'sso.google.redirect_uri';
