-- V3__fix_db_issues.sql
-- 修复数据库审查报告问题（C-2, H-1, H-2, H-3, M-2, M-3, M-5）
-- 所有 DDL 使用 IF NOT EXISTS / IF EXISTS 防止部分失败后重试报错（M-2）

-- ──────────────────────────────────────────────────────────────
-- C-2: tenant_single_device_configs.tenant_id NULL → NOT NULL
--      用系统 sentinel UUID 填充现有 NULL，再改约束
-- ──────────────────────────────────────────────────────────────
UPDATE tenant_single_device_configs
SET tenant_id = '00000000-0000-0000-0000-000000000000'
WHERE tenant_id IS NULL;

-- 先删除依赖 tenant_id NULL 的外键约束，再改列，再重建外键
ALTER TABLE tenant_single_device_configs
    DROP FOREIGN KEY IF EXISTS fk_sdc_tenant;

ALTER TABLE tenant_single_device_configs
    MODIFY COLUMN tenant_id CHAR(36) NOT NULL;

-- sentinel 租户必须存在才能重建外键；若尚未插入则插入
INSERT IGNORE INTO tenants (id, code, name, type, status, default_login_type)
VALUES ('00000000-0000-0000-0000-000000000000',
        'sentinel', '系统 Sentinel 租户', 'ADMIN', 'INACTIVE', 'PASSWORD');

ALTER TABLE tenant_single_device_configs
    ADD CONSTRAINT IF NOT EXISTS fk_sdc_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- ──────────────────────────────────────────────────────────────
-- H-1: tenant_benefit_packages.package_id 缺索引
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_tbp_package_id
    ON tenant_benefit_packages (package_id);

-- ──────────────────────────────────────────────────────────────
-- H-2: promotional_slots 缺复合索引（tenant_id, status, sort_order）
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_ps_tenant_status_sort
    ON promotional_slots (tenant_id, status, sort_order);

-- ──────────────────────────────────────────────────────────────
-- H-3: accounts 缺 (tenant_id, name) 索引，支持后缀 LIKE 优化
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_accounts_tenant_name
    ON accounts (tenant_id, name);

-- ──────────────────────────────────────────────────────────────
-- M-3: tenant_permissions.permission_id 缺索引
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_tp_permission_id
    ON tenant_permissions (permission_id);

-- ──────────────────────────────────────────────────────────────
-- M-3: role_permissions.permission_id 缺索引
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_rp_permission_id
    ON role_permissions (permission_id);

-- ──────────────────────────────────────────────────────────────
-- M-5: tenant_sso_configs.client_secret 扩容至 VARCHAR(1024)
--      以容纳 AES-GCM 加密后的 Base64 密文
-- ──────────────────────────────────────────────────────────────
ALTER TABLE tenant_sso_configs
    MODIFY COLUMN client_secret VARCHAR(1024) NOT NULL;
