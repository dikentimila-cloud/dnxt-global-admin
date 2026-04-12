-- ============================================================================
-- V10 — In-process AI Gateway: provider / model / credential / config
-- Same schema as Operations (V40) so the pattern is consistent across apps.
-- ============================================================================

CREATE TABLE IF NOT EXISTS aig_provider (
    provider_id          VARCHAR(40)  PRIMARY KEY,
    provider_name        VARCHAR(100) NOT NULL UNIQUE,
    display_name         VARCHAR(150),
    provider_type        VARCHAR(40)  NOT NULL,
    base_endpoint        VARCHAR(500),
    api_version          VARCHAR(40),
    is_active            VARCHAR(1)   NOT NULL DEFAULT 'Y',
    supports_streaming   VARCHAR(1)   NOT NULL DEFAULT 'N',
    supports_functions   VARCHAR(1)   NOT NULL DEFAULT 'N',
    supports_vision      VARCHAR(1)   NOT NULL DEFAULT 'N',
    created_by           VARCHAR(100),
    created_date         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    modified_by          VARCHAR(100),
    modified_date        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aig_provider_type   ON aig_provider(provider_type);
CREATE INDEX IF NOT EXISTS idx_aig_provider_active ON aig_provider(is_active);

CREATE TABLE IF NOT EXISTS aig_model (
    model_id             VARCHAR(40)  PRIMARY KEY,
    provider_id          VARCHAR(40)  NOT NULL REFERENCES aig_provider(provider_id) ON DELETE CASCADE,
    model_name           VARCHAR(150) NOT NULL,
    model_display_name   VARCHAR(200),
    model_type           VARCHAR(30)  NOT NULL DEFAULT 'CHAT',
    deployment_name      VARCHAR(200),
    max_input_tokens     BIGINT       DEFAULT 128000,
    max_output_tokens    BIGINT       DEFAULT 4096,
    cost_per_input_1k    NUMERIC(12,6) DEFAULT 0,
    cost_per_output_1k   NUMERIC(12,6) DEFAULT 0,
    is_active            VARCHAR(1)   NOT NULL DEFAULT 'Y',
    created_by           VARCHAR(100),
    created_date         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    modified_by          VARCHAR(100),
    modified_date        TIMESTAMP,
    CONSTRAINT uk_aig_model_per_provider UNIQUE (provider_id, model_name)
);

CREATE INDEX IF NOT EXISTS idx_aig_model_provider ON aig_model(provider_id);
CREATE INDEX IF NOT EXISTS idx_aig_model_type     ON aig_model(model_type);

CREATE TABLE IF NOT EXISTS aig_credential (
    credential_id        VARCHAR(40)  PRIMARY KEY,
    provider_id          VARCHAR(40)  NOT NULL UNIQUE REFERENCES aig_provider(provider_id) ON DELETE CASCADE,
    api_key_encrypted    VARCHAR(2000),
    endpoint_override    VARCHAR(500),
    resource_name        VARCHAR(200),
    is_active            VARCHAR(1)   NOT NULL DEFAULT 'Y',
    created_by           VARCHAR(100),
    created_date         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    modified_by          VARCHAR(100),
    modified_date        TIMESTAMP
);

CREATE TABLE IF NOT EXISTS aig_tenant_config (
    config_id                VARCHAR(40)  PRIMARY KEY,
    ai_enabled               VARCHAR(1)   NOT NULL DEFAULT 'N',
    default_provider_id      VARCHAR(40)  REFERENCES aig_provider(provider_id) ON DELETE SET NULL,
    default_model_id         VARCHAR(40)  REFERENCES aig_model(model_id)       ON DELETE SET NULL,
    max_requests_per_minute  INTEGER,
    max_cost_per_day         NUMERIC(12,2),
    max_cost_per_month       NUMERIC(12,2),
    cost_alert_threshold     INTEGER,
    modified_by              VARCHAR(100),
    modified_date            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO aig_tenant_config (config_id, ai_enabled)
VALUES ('default', 'N')
ON CONFLICT (config_id) DO NOTHING;

-- Remove dead AI feature toggles from platform_config (now in aig_* tables)
DELETE FROM platform_config WHERE config_key LIKE 'ai.%';
