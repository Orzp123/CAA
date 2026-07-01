# Data Model: feature/002 学校租户管理与账户管理

**日期**: 2026-06-25
**Migration**: V2__school_tenant_mgmt.sql
**依赖**: V1__auth_schema.sql（tenants、accounts、permissions 已存在）

---

## 变更概览

| 操作 | 表 | 说明 |
|------|-----|------|
| ALTER | `tenants` | 增加品牌字段（logo_url、system_name_zh/en、description） |
| ALTER | `accounts` | 增加 email、phone、secondary_role |
| CREATE | `benefit_packages` | 权益套餐主表 |
| CREATE | `tenant_benefit_packages` | 租户-套餐关联 |
| CREATE | `promotional_slots` | 运营位 |
| INSERT | `benefit_packages` | 预置标准版/专业版套餐 |

---

## 实体详情

### 1. tenants（扩展）

新增列：

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| logo_url | VARCHAR(512) | NULL | MinIO 存储路径 |
| system_name_zh | VARCHAR(128) | NULL | 系统名称（中文） |
| system_name_en | VARCHAR(128) | NULL | 系统名称（英文） |
| description | VARCHAR(1024) | NULL | 系统描述 |

**状态转换**：ACTIVE ↔ INACTIVE（可反复切换）

---

### 2. accounts（扩展）

新增列：

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| email | VARCHAR(256) | NULL | 邮箱（基础格式校验） |
| phone | VARCHAR(32) | NULL | 手机号（基础格式校验） |
| secondary_role | ENUM('TEACHER','ASSISTANT') | NULL | 第二身份，创建后不可修改 |

**第二身份校验规则**（服务层强制）：
- account_type=SCHOOL_ADMIN → secondary_role ∈ {TEACHER, ASSISTANT, NULL}
- account_type=TEACHER → secondary_role ∈ {ASSISTANT, NULL}
- account_type=STUDENT → secondary_role ∈ {ASSISTANT, NULL}
- account_type=SYSTEM_ADMIN → secondary_role 必须为 NULL

**字段格式校验**（服务层）：
- email: `^[^@\s]+@[^@\s]+\.[^@\s]+$`
- phone: `^1[3-9]\d{9}$`

---

### 3. benefit_packages（新建）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK | UUID |
| code | VARCHAR(64) | UNIQUE NOT NULL | 套餐编码（STANDARD/PRO） |
| name | VARCHAR(128) | NOT NULL | 套餐名称 |
| storage_gb | INT | NOT NULL | 存储空间上限（GB） |
| max_agents | INT | NOT NULL | Agent 数量上限（-1 不限） |
| default_permission_codes | JSON | NOT NULL | 默认勾选权限 code 列表 |
| status | ENUM('ACTIVE','INACTIVE') | NOT NULL DEFAULT 'ACTIVE' | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### 4. tenant_benefit_packages（新建）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK | UUID |
| tenant_id | CHAR(36) | FK→tenants, UNIQUE NOT NULL | 一租户同时只关联一个套餐 |
| package_id | CHAR(36) | FK→benefit_packages NOT NULL | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### 5. promotional_slots（新建）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK | UUID |
| tenant_id | CHAR(36) | FK→tenants NOT NULL | |
| title | VARCHAR(128) | NOT NULL | 标题 |
| image_url | VARCHAR(512) | NULL | 图片（MinIO 路径） |
| link_url | VARCHAR(1024) | NULL | 跳转链接（不校验格式） |
| position | ENUM('HOME_TOP_BANNER','HOME_SIDEBAR','LOGIN_BANNER') | NOT NULL | |
| sort_order | INT | NOT NULL DEFAULT 0 | 越小越靠前 |
| status | ENUM('ACTIVE','INACTIVE') | NOT NULL DEFAULT 'ACTIVE' | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

约束：每个租户运营位数量 ≤ 10（服务层校验）

---

## V2 Migration SQL

```sql
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
```

---

## 实体关系

```
tenants (1) ──── (0..1) tenant_benefit_packages ──── (N:1) benefit_packages
tenants (1) ──── (0..10) promotional_slots
tenants (1) ──── (N) accounts
accounts.secondary_role → 服务层规则约束（不建外键，ENUM 自约束）
```

---

## 技术债记录

### TD-001: UUID v4 → v7 迁移计划

**登记日期**: 2026-06-29
**优先级**: 中（P2）
**影响范围**: 所有使用 `CHAR(36)` 主键的表（tenants、accounts、permissions 等共 10+ 张表）

**问题描述**:
当前全库主键使用 UUID v4（随机生成），导致 InnoDB 聚簇索引页分裂严重，大量数据写入时 B-Tree 碎片化，影响插入性能。

**目标**:
迁移至 UUID v7（时间有序 UUID，RFC 9562），利用时间戳前缀保证单调递增，显著降低页分裂频率，提升写入吞吐。

**迁移步骤（计划）**:
1. 引入 UUID v7 生成工具库（如 `com.fasterxml.uuid:java-uuid-generator` 4.x）
2. 修改 Java 实体层 `@PrePersist` 的 UUID 生成逻辑，改用 `Generators.timeBasedEpochGenerator().generate()`
3. **存量数据无需迁移**：UUID v7 与 v4 格式兼容（同为 `CHAR(36)`），新记录用 v7，旧记录保持 v4 不变
4. 确认所有外键引用列类型不变，无需 schema 变更
5. 在压测环境验证写入 TPS 提升，目标降低页分裂率 ≥ 50%

**前置条件**:
- 需在 CAA 整体流量低谷窗口上线，避免混合 UUID 版本对排序查询造成困惑
- 需更新 `JpaConfig` 或 `BaseEntity` 的 ID 生成策略

**预计工作量**: 0.5 人天
**负责人**: 待分配
