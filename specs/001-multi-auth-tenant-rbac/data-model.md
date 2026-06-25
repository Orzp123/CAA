# Data Model: 多租户用户认证与权限管理系统

**Created**: 2026-06-17
**Feature**: 001-multi-auth-tenant-rbac

---

## 实体关系概览

```
Tenant (1) ──── (N) Account
Tenant (1) ──── (1) TenantLoginConfig
Tenant (1) ──── (N) TenantPermission
Tenant (1) ──── (0..1) TenantSsoConfig
Account (1) ──── (N) AccountRolePermission
Permission (N) ──── (N) TenantPermission
Permission (N) ──── (N) AccountRolePermission
SystemConfig (1) ──── 全局配置（单行）
```

---

## 表定义

### tenants（租户）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | 租户唯一 ID |
| code | VARCHAR(64) | UNIQUE, NOT NULL | 租户标识码（用于域名前缀） |
| name | VARCHAR(128) | NOT NULL | 租户名称（学校名） |
| type | ENUM | NOT NULL | ADMIN / SCHOOL / WECHAT |
| status | ENUM | NOT NULL | ACTIVE / INACTIVE |
| domain | VARCHAR(256) | NULL | 绑定的子域名 |
| default_login_type | ENUM | NOT NULL | PASSWORD / WECHAT / SSO |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引**：
- `idx_tenants_code` (code)
- `idx_tenants_domain` (domain)
- `idx_tenants_status` (status)

---

### tenant_login_configs（租户登录方式配置）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | |
| tenant_id | CHAR(36) | FK→tenants, NOT NULL | |
| login_type | ENUM | NOT NULL | PASSWORD / WECHAT / SSO |
| enabled | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否启用 |
| is_default | TINYINT(1) | NOT NULL, DEFAULT 0 | 是否为默认登录方式 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**约束**：每个 tenant_id 有且仅有一条 is_default=1 的记录
**索引**：`uk_tenant_login_type` UNIQUE (tenant_id, login_type)

---

### tenant_sso_configs（租户 SSO 配置）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | |
| tenant_id | CHAR(36) | FK→tenants, UNIQUE | 一个租户一条 SSO 配置 |
| issuer_uri | VARCHAR(512) | NOT NULL | OIDC Discovery 地址 |
| client_id | VARCHAR(256) | NOT NULL | |
| client_secret | VARCHAR(512) | NOT NULL | 加密存储 |
| scope | VARCHAR(256) | NOT NULL | openid profile email |
| role_claim | VARCHAR(128) | NULL | IdP 中表示账户类型的 claim 名称 |
| role_mapping | JSON | NULL | IdP 角色值 → 系统账户类型映射 |
| enabled | TINYINT(1) | NOT NULL, DEFAULT 1 | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

### accounts（账户）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | 平台唯一 ID |
| tenant_id | CHAR(36) | FK→tenants, NOT NULL | 所属租户 |
| student_no | VARCHAR(64) | NOT NULL | 学号 |
| name | VARCHAR(128) | NOT NULL | 姓名 |
| nickname | VARCHAR(128) | NULL | 昵称，NULL 时显示层使用 name |
| account_type | ENUM | NOT NULL | SYSTEM_ADMIN / SCHOOL_ADMIN / TEACHER / STUDENT |
| status | ENUM | NOT NULL | ACTIVE / DISABLED / LOCKED |
| password_hash | VARCHAR(256) | NULL | 账号密码登录时必填，BCrypt |
| wechat_openid | VARCHAR(128) | NULL | 微信 openid |
| wechat_unionid | VARCHAR(128) | NULL | 微信 unionid |
| sso_subject | VARCHAR(256) | NULL | SSO IdP subject |
| login_fail_count | INT | NOT NULL, DEFAULT 0 | 连续登录失败次数 |
| locked_until | DATETIME | NULL | 锁定到期时间，NULL 表示未锁定 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**唯一约束**：
- `uk_account_tenant_student_no` UNIQUE (tenant_id, student_no)
- `uk_account_wechat_openid` UNIQUE (wechat_openid) WHERE wechat_openid IS NOT NULL
- `uk_account_sso_subject` UNIQUE (tenant_id, sso_subject) WHERE sso_subject IS NOT NULL

**索引**：
- `idx_accounts_tenant_id` (tenant_id)
- `idx_accounts_status` (status)
- `idx_accounts_account_type` (account_type)

---

### permissions（权限/功能模块定义）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | |
| code | VARCHAR(128) | UNIQUE, NOT NULL | 权限标识码（如 AGENT_MANAGE） |
| name | VARCHAR(128) | NOT NULL | 显示名称 |
| module | VARCHAR(64) | NOT NULL | 所属功能模块（如 AGENT） |
| action | VARCHAR(64) | NOT NULL | 操作类型（如 READ / WRITE / DELETE） |
| status | ENUM | NOT NULL | ACTIVE / INACTIVE |
| is_system | TINYINT(1) | NOT NULL, DEFAULT 0 | 系统内置，不可删除 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_permissions_module` (module)

---

### tenant_permissions（租户功能模块权限配置）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | |
| tenant_id | CHAR(36) | FK→tenants, NOT NULL | |
| permission_id | CHAR(36) | FK→permissions, NOT NULL | |
| enabled | TINYINT(1) | NOT NULL, DEFAULT 1 | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**唯一约束**：`uk_tenant_permission` UNIQUE (tenant_id, permission_id)

---

### role_permissions（角色权限配置）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | |
| tenant_id | CHAR(36) | FK→tenants, NOT NULL | |
| account_type | ENUM | NOT NULL | SCHOOL_ADMIN / TEACHER / STUDENT |
| permission_id | CHAR(36) | FK→permissions, NOT NULL | |
| enabled | TINYINT(1) | NOT NULL, DEFAULT 1 | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**说明**：SYSTEM_ADMIN 权限硬编码，不存此表
**唯一约束**：`uk_role_permission` UNIQUE (tenant_id, account_type, permission_id)

---

### system_configs（系统全局配置）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| config_key | VARCHAR(128) | PK | 配置键 |
| config_value | VARCHAR(1024) | NOT NULL | 配置值（JSON 或字符串） |
| description | VARCHAR(256) | NULL | 说明 |
| updated_at | DATETIME | NOT NULL | |

**预置配置键**：

| config_key | 默认值 | 说明 |
|------------|--------|------|
| jwt.algorithm | HS256 | Token 签名算法（Nacos 热加载） |
| jwt.expiration_seconds | 7200 | Token 过期时间（秒） |
| login.max_fail_count | 5 | 最大连续失败次数 |
| login.lock_duration_seconds | 900 | 锁定时长（秒） |
| single_device.enabled | false | 全局单设备限制开关 |
| captcha.expiration_seconds | 300 | 验证码有效期（秒） |

---

### tenant_single_device_configs（租户/角色级单设备限制配置）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | CHAR(36) | PK, UUID | |
| tenant_id | CHAR(36) | FK→tenants, NULL | NULL 表示全局配置覆盖 |
| account_type | ENUM | NULL | NULL 表示租户级，有值表示账户类型级 |
| enabled | TINYINT(1) | NOT NULL | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**唯一约束**：`uk_single_device_config` UNIQUE (tenant_id, account_type)

---

## 账户状态流转

```
[初始] ACTIVE
  │
  ├─ 管理员禁用 ──────────────► DISABLED（所有 Token 加入黑名单）
  │                               │
  │                               └─ 管理员恢复 ──► ACTIVE
  │
  └─ 连续失败 ≥ 阈值 ──────────► LOCKED（login_fail_count 重置，locked_until 设置）
                                  │
                                  └─ 超时自动恢复 ──► ACTIVE（locked_until < NOW()）
```

---

## Redis 缓存设计

| Key 模式 | 类型 | TTL | 说明 |
|----------|------|-----|------|
| `token:blacklist:{jti}` | String | Token 剩余有效期 | Token 黑名单 |
| `token:active:{accountId}:{deviceType}` | String | Token 过期时间 | 单设备活跃 Token JTI |
| `tenant:domain:{domain}` | String | 5min | 域名→租户ID 映射 |
| `tenant:info:{tenantId}` | Hash | 10min | 租户基本信息 |
| `permission:tenant:{tenantId}` | Set | 10min | 租户已开放的 permission_code 集合 |
| `permission:role:{tenantId}:{roleType}` | Set | 10min | 角色已开放的 permission_code 集合 |
| `captcha:{uuid}` | String | 5min | 验证码（一次性） |
| `singledevice:config:{tenantId}:{roleType}` | String | 5min | 单设备配置（三层合并结果） |

---

## 新增模块位置

```
com.caa
└── auth/                          新增模块
    ├── model/
    │   ├── Tenant.java
    │   ├── Account.java
    │   ├── Permission.java
    │   ├── TenantPermission.java
    │   ├── RolePermission.java
    │   ├── TenantLoginConfig.java
    │   ├── TenantSsoConfig.java
    │   ├── SystemConfig.java
    │   └── TenantSingleDeviceConfig.java
    ├── dto/
    │   ├── LoginRequest.java (record)
    │   ├── LoginResponse.java (record)
    │   ├── WechatCallbackRequest.java (record)
    │   ├── SsoCallbackRequest.java (record)
    │   └── TokenInfo.java (record)
    ├── repository/
    │   ├── TenantRepository.java
    │   ├── AccountRepository.java
    │   ├── PermissionRepository.java
    │   └── ...
    ├── service/
    │   ├── AuthService.java
    │   ├── TenantService.java
    │   ├── PermissionService.java
    │   ├── TokenService.java
    │   ├── CaptchaService.java
    │   └── wechat/WechatOAuth2Service.java
    └── controller/
        ├── AuthController.java
        ├── TenantController.java
        └── PermissionController.java

gateway/                           新增服务模块
└── src/main/java/com/caa/gateway/
    ├── filter/
    │   ├── AuthenticationFilter.java
    │   └── TenantResolutionFilter.java
    └── config/
        └── GatewayRouteConfig.java
```
