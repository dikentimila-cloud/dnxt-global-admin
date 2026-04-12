-- ============================================================================
-- V11 — AI Usage Tracking: per-tenant, per-request metering
-- Logs every AI call with tenant, provider, model, tokens, cost.
-- ============================================================================

CREATE TABLE IF NOT EXISTS aig_usage_log (
    usage_id             VARCHAR(40)   PRIMARY KEY,
    tenant_id            VARCHAR(36),
    tenant_name          VARCHAR(200),
    provider_id          VARCHAR(40)   REFERENCES aig_provider(provider_id) ON DELETE SET NULL,
    provider_name        VARCHAR(150),
    model_id             VARCHAR(40)   REFERENCES aig_model(model_id) ON DELETE SET NULL,
    model_name           VARCHAR(150),
    input_tokens         INTEGER       DEFAULT 0,
    output_tokens        INTEGER       DEFAULT 0,
    total_tokens         INTEGER       DEFAULT 0,
    cost_input           NUMERIC(12,6) DEFAULT 0,
    cost_output          NUMERIC(12,6) DEFAULT 0,
    cost_total           NUMERIC(12,6) DEFAULT 0,
    request_type         VARCHAR(50),
    status               VARCHAR(20)   DEFAULT 'SUCCESS',
    error_message        VARCHAR(1000),
    duration_ms          INTEGER,
    requested_by         VARCHAR(100),
    created_date         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aig_usage_tenant  ON aig_usage_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_aig_usage_date    ON aig_usage_log(created_date);
CREATE INDEX IF NOT EXISTS idx_aig_usage_provider ON aig_usage_log(provider_id);
CREATE INDEX IF NOT EXISTS idx_aig_usage_status  ON aig_usage_log(status);

-- Summary view: daily cost per tenant
CREATE OR REPLACE VIEW aig_usage_daily AS
SELECT
    tenant_id,
    tenant_name,
    DATE(created_date) AS usage_date,
    COUNT(*)           AS total_requests,
    SUM(input_tokens)  AS total_input_tokens,
    SUM(output_tokens) AS total_output_tokens,
    SUM(total_tokens)  AS total_tokens,
    SUM(cost_total)    AS total_cost,
    COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END) AS success_count,
    COUNT(CASE WHEN status = 'ERROR' THEN 1 END)   AS error_count
FROM aig_usage_log
GROUP BY tenant_id, tenant_name, DATE(created_date);
