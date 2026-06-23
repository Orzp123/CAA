-- V1__auth_schema.sql
-- Multi-tenant auth & RBAC schema

CREATE TABLE tenants (
    id                 CHAR(36)     NOT NULL,
    code               VARCHAR(64)  NOT NULL,
    name               VARCHAR(128) NOT NULL,
    type               ENUM('ADMIN','SCHOOL','WECHAT') NOT NULL,
    status             ENUM('ACTIVE','INACTIVE')       NOT NULL DEFAULT 'ACTIVE',
    domain             VARCHAR(256) NULL,
    default_login_type ENUM('PASSWORD','WECHAT','SSO') NOT NULL DEFAULT 'PASSWORD',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenants_code (code),
    INDEX idx_tenants_domain (domain),
    INDEX idx_tenants_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tenant_login_configs (
    id           CHAR(36)   NOT NULL,
    tenant_id    CHAR(36)   NOT NULL,
    login_type   ENUM('PASSWORD','WECHAT','SSO') NOT NULL,
    enabled      TINYINT(1) NOT NULL DEFAULT 1,
    is_default   TINYINT(1) NOT NULL DEFAULT 0,
    created_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_login_type (tenant_id, login_type),
    CONSTRAINT fk_tlc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tenant_sso_configs (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    issuer_uri    VARCHAR(512) NOT NULL,
    client_id     VARCHAR(256) NOT NULL,
    client_secret VARCHAR(512) NOT NULL,
    scope         VARCHAR(256) NOT NULL DEFAULT 'openid profile email',
    role_claim    VARCHAR(128) NULL,
    role_mapping  JSON         NULL,
    enabled       TINYINT(1)  NOT NULL DEFAULT 1,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_sso (tenant_id),
    CONSTRAINT fk_tsc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE accounts (
    id               CHAR(36)     NOT NULL,
    tenant_id        CHAR(36)     NOT NULL,
    student_no       VARCHAR(64)  NOT NULL,
    name             VARCHAR(128) NOT NULL,
    nickname         VARCHAR(128) NULL,
    account_type     ENUM('SYSTEM_ADMIN','SCHOOL_ADMIN','TEACHER','STUDENT') NOT NULL,
    status           ENUM('ACTIVE','DISABLED','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    password_hash    VARCHAR(256) NULL,
    wechat_openid    VARCHAR(128) NULL,
    wechat_unionid   VARCHAR(128) NULL,
    sso_subject      VARCHAR(256) NULL,
    login_fail_count INT          NOT NULL DEFAULT 0,
    locked_until     DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_tenant_student_no (tenant_id, student_no),
    UNIQUE KEY uk_account_wechat_openid (wechat_openid),
    INDEX idx_accounts_tenant_id (tenant_id),
    INDEX idx_accounts_status (status),
    INDEX idx_accounts_account_type (account_type),
    CONSTRAINT fk_acc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
    id         CHAR(36)     NOT NULL,
    code       VARCHAR(128) NOT NULL,
    name       VARCHAR(128) NOT NULL,
    module     VARCHAR(64)  NOT NULL,
    action     VARCHAR(64)  NOT NULL,
    status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    is_system  TINYINT(1)  NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code),
    INDEX idx_permissions_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tenant_permissions (
    id            CHAR(36)   NOT NULL,
    tenant_id     CHAR(36)   NOT NULL,
    permission_id CHAR(36)   NOT NULL,
    enabled       TINYINT(1) NOT NULL DEFAULT 1,
    created_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_permission (tenant_id, permission_id),
    CONSTRAINT fk_tp_tenant     FOREIGN KEY (tenant_id)     REFERENCES tenants(id),
    CONSTRAINT fk_tp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    id            CHAR(36)   NOT NULL,
    tenant_id     CHAR(36)   NOT NULL,
    account_type  ENUM('SCHOOL_ADMIN','TEACHER','STUDENT') NOT NULL,
    permission_id CHAR(36)   NOT NULL,
    enabled       TINYINT(1) NOT NULL DEFAULT 1,
    created_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (tenant_id, account_type, permission_id),
    CONSTRAINT fk_rp_tenant     FOREIGN KEY (tenant_id)     REFERENCES tenants(id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_configs (
    config_key   VARCHAR(128)  NOT NULL,
    config_value VARCHAR(1024) NOT NULL,
    description  VARCHAR(256)  NULL,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tenant_single_device_configs (
    id           CHAR(36)   NOT NULL,
    tenant_id    CHAR(36)   NULL,
    account_type ENUM('SYSTEM_ADMIN','SCHOOL_ADMIN','TEACHER','STUDENT') NULL,
    enabled      TINYINT(1) NOT NULL,
    created_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_single_device_config (tenant_id, account_type),
    CONSTRAINT fk_sdc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed: system config defaults
INSERT INTO system_configs (config_key, config_value, description) VALUES
('jwt.algorithm',              'HS256', 'JWT signing algorithm (hot-reload via Nacos)'),
('jwt.expiration_seconds',     '7200',  'Token TTL in seconds'),
('login.max_fail_count',       '5',     'Max consecutive login failures before lock'),
('login.lock_duration_seconds','900',   'Account lock duration in seconds'),
('single_device.enabled',      'false', 'Global single-device restriction switch'),
('captcha.expiration_seconds', '300',   'Captcha TTL in seconds');

-- Seed: built-in tenants
INSERT INTO tenants (id, code, name, type, status, default_login_type) VALUES
('00000000-0000-0000-0000-000000000001', 'admin',  'CAA 系统管理', 'ADMIN',  'ACTIVE', 'PASSWORD'),
('00000000-0000-0000-0000-000000000002', 'wechat', '微信大学',     'WECHAT', 'ACTIVE', 'WECHAT');

-- Seed: default permissions (is_system=1, not deletable)
INSERT INTO permissions (id, code, name, module, action, is_system) VALUES
('10000000-0000-0000-0000-000000000001', 'AGENT_READ',     'Agent 查看',  'AGENT',    'READ',  1),
('10000000-0000-0000-0000-000000000002', 'AGENT_WRITE',    'Agent 管理',  'AGENT',    'WRITE', 1),
('10000000-0000-0000-0000-000000000003', 'WORKFLOW_READ',  '工作流查看',  'WORKFLOW', 'READ',  1),
('10000000-0000-0000-0000-000000000004', 'WORKFLOW_WRITE', '工作流管理',  'WORKFLOW', 'WRITE', 1),
('10000000-0000-0000-0000-000000000005', 'PAGE_READ',      '页面查看',    'PAGE',     'READ',  1),
('10000000-0000-0000-0000-000000000006', 'PAGE_WRITE',     '页面管理',    'PAGE',     'WRITE', 1),
('10000000-0000-0000-0000-000000000007', 'CHAT_USE',       '对话使用',    'CHAT',     'USE',   1);
