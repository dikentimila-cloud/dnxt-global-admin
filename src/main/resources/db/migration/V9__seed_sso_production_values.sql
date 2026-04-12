-- =============================================================================
-- Seed production SSO values into platform_config.
-- DB is the single source of truth — no env var fallback.
--
-- NOTE: Actual values (client_id, client_secret, redirect_uri) are seeded
-- manually via the Settings > Single Sign-On page after deploy. This
-- placeholder migration exists so the Flyway version sequence stays intact.
-- =============================================================================
SELECT 1;
