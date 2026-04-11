-- =============================================================================
-- DnXT Global Admin Portal — Core Schema
-- Enterprise-grade: full audit trail, account lockout, email domain restriction
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------------
CREATE TABLE admin_role (
    role_id         VARCHAR(36) PRIMARY KEY,
    role_name       VARCHAR(100) NOT NULL UNIQUE,
    role_label      VARCHAR(200) NOT NULL,
    description     VARCHAR(500),
    is_system       BOOLEAN DEFAULT FALSE,
    created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- Permissions
-- ---------------------------------------------------------------------------
CREATE TABLE admin_permission (
    permission_id   VARCHAR(36) PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    module          VARCHAR(50)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    description     VARCHAR(500),
    created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- Role ↔ Permission join
-- ---------------------------------------------------------------------------
CREATE TABLE admin_role_permission (
    role_id         VARCHAR(36) NOT NULL,
    permission_id   VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES admin_role(role_id),
    FOREIGN KEY (permission_id) REFERENCES admin_permission(permission_id)
);

-- ---------------------------------------------------------------------------
-- Users (DnXT staff only — email domain enforced at app layer)
-- ---------------------------------------------------------------------------
CREATE TABLE admin_user (
    user_id         VARCHAR(36) PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    role_id         VARCHAR(36),
    is_active       BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT TRUE,
    last_login      TIMESTAMP,
    failed_attempts INT DEFAULT 0,
    locked_until    TIMESTAMP,
    created_by      VARCHAR(36),
    created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by     VARCHAR(36),
    modified_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES admin_role(role_id)
);

-- ---------------------------------------------------------------------------
-- Platform Tenants (the organizations using DnXT products)
-- ---------------------------------------------------------------------------
CREATE TABLE platform_tenant (
    tenant_id       VARCHAR(36) PRIMARY KEY,
    tenant_name     VARCHAR(200) NOT NULL,
    tenant_slug     VARCHAR(100) NOT NULL UNIQUE,
    domain          VARCHAR(255),
    industry        VARCHAR(100),
    logo_url        VARCHAR(500),
    primary_contact_name  VARCHAR(200),
    primary_contact_email VARCHAR(255),
    phone           VARCHAR(50),
    address         TEXT,
    status          VARCHAR(20) DEFAULT 'Active',
    license_type    VARCHAR(50),
    license_expiry  DATE,
    max_users       INT DEFAULT 50,
    notes           TEXT,
    created_by      VARCHAR(36),
    created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by     VARCHAR(36),
    modified_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN DEFAULT TRUE
);

-- ---------------------------------------------------------------------------
-- Tenant ↔ Module licensing (which products each tenant has)
-- ---------------------------------------------------------------------------
CREATE TABLE tenant_module (
    module_id       VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(36) NOT NULL,
    module_name     VARCHAR(50) NOT NULL,
    is_enabled      BOOLEAN DEFAULT FALSE,
    licensed_users  INT DEFAULT 0,
    activated_date  DATE,
    expiry_date     DATE,
    config_json     TEXT,
    created_by      VARCHAR(36),
    created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by     VARCHAR(36),
    modified_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES platform_tenant(tenant_id),
    UNIQUE (tenant_id, module_name)
);

-- ---------------------------------------------------------------------------
-- Platform Configuration (key-value store for system settings)
-- ---------------------------------------------------------------------------
CREATE TABLE platform_config (
    config_key      VARCHAR(200) PRIMARY KEY,
    config_value    TEXT,
    category        VARCHAR(50) DEFAULT 'GENERAL',
    description     VARCHAR(500),
    is_secret       BOOLEAN DEFAULT FALSE,
    modified_by     VARCHAR(36),
    modified_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- Audit Log (every admin action recorded — security critical)
-- ---------------------------------------------------------------------------
CREATE TABLE audit_log (
    log_id          VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36),
    username        VARCHAR(100),
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(50),
    target_id       VARCHAR(36),
    target_name     VARCHAR(200),
    details         TEXT,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------
CREATE INDEX idx_admin_user_email ON admin_user(email);
CREATE INDEX idx_admin_user_role ON admin_user(role_id);
CREATE INDEX idx_platform_tenant_slug ON platform_tenant(tenant_slug);
CREATE INDEX idx_platform_tenant_status ON platform_tenant(status);
CREATE INDEX idx_tenant_module_tenant ON tenant_module(tenant_id);
CREATE INDEX idx_tenant_module_name ON tenant_module(module_name);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_date ON audit_log(created_date);
CREATE INDEX idx_audit_log_target ON audit_log(target_type, target_id);
