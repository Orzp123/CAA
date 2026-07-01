-- V2__school_tenant_mgmt.sql

-- 1. 扩展 tenants 品牌字段
ALTER TABLE tenants
    ADD COLUMN logo_url        VARCHAR(512)  NULL AFTER domain,
    ADD COLUMN system_name_zh  VARCHAR(128)  NULL AFTER logo_url,
    ADD COLUMN system_name_en  VARCHAR(128)  NULL AFTER system_name_zh,
    ADD COLUMN description     VARCHAR(1024) NULL AFTER system_name_en;

-- 2. 扩展 accounts 字段
ALTER TABLE accounts
    ADD COLUMN email          VARCHAR(256) NULL AFTER nickname,
    ADD COLUMN phone          VARCHAR(32)  NULL AFTER email,
    ADD COLUMN secondary_role ENUM('TEACHER','ASSISTANT') NULL AFTER account_type;

-- 3. 权益套餐主表
CREATE TABLE benefit_packages (
    id                       CHAR(36)     NOT NULL,
    code                     VARCHAR(64)  NOT NULL,
    name                     VARCHAR(128) NOT NULL,
    storage_gb               INT          NOT NULL DEFAULT 10,
    max_agents               INT          NOT NULL DEFAULT 10,
    default_permission_codes JSON         NOT NULL,
    status                   ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_benefit_packages_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 租户-套餐关联
CREATE TABLE tenant_benefit_packages (
    id         CHAR(36) NOT NULL,
    tenant_id  CHAR(36) NOT NULL,
    package_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_package (tenant_id),
    CONSTRAINT fk_tbp_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants(id),
    CONSTRAINT fk_tbp_package FOREIGN KEY (package_id) REFERENCES benefit_packages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 运营位
CREATE TABLE promotional_slots (
    id         CHAR(36)      NOT NULL,
    tenant_id  CHAR(36)      NOT NULL,
    title      VARCHAR(128)  NOT NULL,
    image_url  VARCHAR(512)  NULL,
    link_url   VARCHAR(1024) NULL,
    position   ENUM('HOME_TOP_BANNER','HOME_SIDEBAR','LOGIN_BANNER') NOT NULL,
    sort_order INT           NOT NULL DEFAULT 0,
    status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_promotional_slots_tenant   (tenant_id),
    INDEX idx_promotional_slots_position (tenant_id, position),
    CONSTRAINT fk_ps_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 预置套餐数据
INSERT INTO benefit_packages (id, code, name, storage_gb, max_agents, default_permission_codes) VALUES
('20000000-0000-0000-0000-000000000001', 'STANDARD', '标准版', 10,  10,  '["AGENT_READ","WORKFLOW_READ","PAGE_READ","CHAT_USE"]'),
('20000000-0000-0000-0000-000000000002', 'PRO',      '专业版', 100, 100, '["AGENT_READ","AGENT_WRITE","WORKFLOW_READ","WORKFLOW_WRITE","PAGE_READ","PAGE_WRITE","CHAT_USE"]');
