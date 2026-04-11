-- =============================================================================
-- Enhanced per-module license configuration
-- Adds license_type (Concurrent/Named) to tenant_module table
-- licensed_users and expiry_date columns already exist
-- =============================================================================

ALTER TABLE tenant_module ADD COLUMN IF NOT EXISTS license_type VARCHAR(20) DEFAULT 'Concurrent';

-- Update the modules list to include all DnXT products
-- (Operations, Reviewer, Publisher, EDMS, Planner already exist from V1)
-- Add Support and Consulting for completeness
