-- =============================================================================
-- Plan-Based Feature Licensing
-- Each module has plans (Starter, Professional, Enterprise) that define
-- which features a tenant can access within that module.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Module Plans — define what features each plan tier includes
-- ---------------------------------------------------------------------------
CREATE TABLE module_plan (
    plan_id       VARCHAR(36) PRIMARY KEY,
    module_name   VARCHAR(50) NOT NULL,
    plan_name     VARCHAR(50) NOT NULL,
    plan_label    VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    features      TEXT NOT NULL,
    sort_order    INT DEFAULT 0,
    is_active     BOOLEAN DEFAULT TRUE,
    created_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(module_name, plan_name)
);

-- ---------------------------------------------------------------------------
-- Operations Plans
-- ---------------------------------------------------------------------------
INSERT INTO module_plan (plan_id, module_name, plan_name, plan_label, description, features, sort_order) VALUES
('plan-ops-starter', 'Operations', 'starter', 'Starter',
 'Core features for small teams — timesheets, employee management, basic admin',
 '["dashboard","timesheets","employees","user-management","settings"]',
 1),
('plan-ops-professional', 'Operations', 'professional', 'Professional',
 'Full business suite — adds CRM, payroll, invoicing, and reporting',
 '["dashboard","timesheets","employees","customers","payroll","invoices","reports","user-management","settings"]',
 2),
('plan-ops-enterprise', 'Operations', 'enterprise', 'Enterprise',
 'Everything — adds revenue analytics, P&L, banking, consulting, workflow automation, and admin tools',
 '["dashboard","timesheets","employees","customers","revenue","pnl","banking","payroll","consulting","invoices","reports","object-manager","workflows","user-management","settings"]',
 3);

-- ---------------------------------------------------------------------------
-- Future: Reviewer, Publisher, EDMS, Planner plans will be added here
-- Example:
-- INSERT INTO module_plan (plan_id, module_name, plan_name, plan_label, description, features, sort_order) VALUES
-- ('plan-rev-starter', 'Reviewer', 'starter', 'Starter', '...', '["document-review","comments"]', 1);
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- Add plan reference + feature overrides to tenant_module
-- ---------------------------------------------------------------------------
ALTER TABLE tenant_module ADD COLUMN IF NOT EXISTS plan_id VARCHAR(36) REFERENCES module_plan(plan_id);
ALTER TABLE tenant_module ADD COLUMN IF NOT EXISTS feature_overrides TEXT DEFAULT '{}';
