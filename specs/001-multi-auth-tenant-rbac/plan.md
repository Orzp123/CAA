# Implementation Plan: 多租户用户认证与权限管理系统

**Feature**: 001-multi-auth-tenant-rbac
**Created**: 2026-06-17
**Spec**: specs/001-multi-auth-tenant-rbac/spec.md
**Status**: Planning Complete

---

## 宪法合规检查

| 原则 | 状态 | 说明 |
|------|------|------|
| 1. Spec-First | ✅ | spec.md 已完成，50/50 检查项通过 |
| 3. API-First | ✅ | 网关 + REST 接口，contracts/api-contracts.md 已定义 |
| 4. Multi-Tenant by Design | ✅ | 所有表含 tenant_id，查询层自动注入租户过滤 |
| 5. Auth Layer Pluggable | ✅ | AuthProvider 接口抽象，支持密码/微信/SSO 插拔 |
| 6. Explicit Over Implicit | ✅ | 所有配置 Nacos 显式声明，无隐式约定 |
| 7. Fail Loud | ✅ | 网关统一 401/403 错误响应，结构化日志 |
| 8. Modularity | ✅ | 新增独立 auth/ 模块和 gateway/ 服务 |
| 11. Test-First | ✅ | 所有任务先写测试 |
| 12. Performance | ✅ | Redis 黑名单 O(1)，权限缓存，热加载配置 |

**ADR 需求**：认证架构从 Phase 1 本地 JWT 扩展为多登录方式，需写 ADR-001

---

## 架构概览

```
[Frontend Next.js :3000]
        │
[Nginx :80]
        │
[Spring Cloud Gateway :8080]  ← 新增服务
   │  Token 验证（Redis 黑名单）
   │  租户识别（Host Header / X-School-Code）
   │  权限粗校验（Redis 缓存）
   │  白名单放行（Nacos 热加载）
        │
[Backend Services :8081]  ← 现有服务端口迁移
   └── auth/    认证、登录、Token 签发
   └── agent/   现有模块
   └── ...      其他现有模块
```

---

## 实现阶段

### 阶段 0：基础设施准备（无业务逻辑）

**目标**：数据库建表、Redis Key 规范、Nacos 配置项、ADR

**任务**：
- T0-1：编写 ADR-001（认证架构决策记录）
- T0-2：编写数据库迁移脚本（Flyway V1__auth_schema.sql）
- T0-3：Nacos 配置项初始化（jwt.*、login.*、captcha.*、single_device.*）
- T0-4：Docker Compose 新增 gateway 服务配置

---

### 阶段 1：核心实体与 Repository（TDD）

**目标**：数据访问层 100% 测试覆盖

**任务**：
- T1-1：Tenant + TenantRepository（含租户状态查询、域名查询）
- T1-2：Account + AccountRepository（含学号唯一约束、锁定查询）
- T1-3：Permission + TenantPermission + RolePermission + Repository
- T1-4：TenantLoginConfig + TenantSsoConfig + Repository
- T1-5：SystemConfig + TenantSingleDeviceConfig + Repository

---

### 阶段 2：Token 服务（TDD）

**目标**：JWT 签发、验证、黑名单、热加载算法切换

**任务**：
- T2-1：TokenService（签发 / 解析 / JTI 生成）
- T2-2：TokenBlacklistService（Redis 黑名单 CRUD）
- T2-3：SingleDeviceService（三层配置合并、活跃 Token 追踪）
- T2-4：Nacos @RefreshScope 集成（算法、过期时间热加载）

---

### 阶段 3：登录服务（TDD）

**目标**：三种登录方式、验证码、账户锁定

**任务**：
- T3-1：CaptchaService（生成 / 验证 / Redis 存储）
- T3-2：PasswordLoginService（学号+学校+密码+验证码，失败计数，锁定）
- T3-3：WechatOAuth2Service（自定义 OAuth2AccessTokenResponseClient，openid 获取，首次登录流程）
- T3-4：SsoOidcService（动态 ClientRegistration，IdP claim 映射，首次登录流程）
- T3-5：AuthService（整合三种登录方式，Token 签发，单设备处理）

---

### 阶段 4：权限服务（TDD）

**目标**：租户权限配置、角色权限配置、Redis 缓存

**任务**：
- T4-1：PermissionService（CRUD，默认模块初始化）
- T4-2：TenantPermissionService（租户功能模块配置，Redis 缓存失效）
- T4-3：RolePermissionService（角色操作权限配置，Redis 缓存失效）
- T4-4：TenantService（租户 CRUD，停用时 Token 处理）

---

### 阶段 5：REST 控制器（TDD）

**目标**：所有 API 端点，Swagger 注解完整

**任务**：
- T5-1：AuthController（/auth/* 全部端点）
- T5-2：TenantController（/admin/tenants/* 全部端点，总 ADMIN 鉴权）
- T5-3：PermissionController（/admin/permissions/* 全部端点）

---

### 阶段 6：Spring Cloud Gateway（TDD）

**目标**：认证网关，Token 验证，权限校验，租户识别，白名单

**任务**：
- T6-1：gateway 模块初始化（spring-cloud-gateway-server 依赖，WebFlux）
- T6-2：TenantResolutionFilter（Host Header 解析 + X-School-Code，Redis 缓存）
- T6-3：AuthenticationFilter（Token 验证，黑名单查询，权限粗校验）
- T6-4：WhitelistConfig（Nacos 热加载白名单路径）
- T6-5：GatewayRouteConfig（路由规则，下游服务发现）

---

### 阶段 7：前端登录页（TDD）

**目标**：登录页 UI，多登录方式切换，验证码，Token 存储

**任务**：
- T7-1：调用 `/frontend-design` 技能设计登录页 UI 方案
- T7-2：LoginPage 组件（多登录方式切换，条件渲染）
- T7-3：PasswordLoginForm（学号/学校/密码/验证码，图片点击刷新）
- T7-4：WechatLoginButton（微信授权跳转）
- T7-5：SsoLoginButton（SSO 跳转）
- T7-6：authApi.ts（登录/登出/租户配置接口封装）
- T7-7：useAuth Zustand store（Token 存储、过期提示、跳转逻辑）

---

### 阶段 8：权限管理前端页面

**目标**：总 ADMIN 权限配置页面

**任务**：
- T8-1：调用 `/frontend-design` 技能设计权限管理页 UI 方案
- T8-2：TenantPermissionPage（租户功能模块配置）
- T8-3：RolePermissionPage（角色权限配置）
- T8-4：SingleDeviceConfigPage（单设备限制配置）

---

### 阶段 9：集成测试与质量门禁

**任务**：
- T9-1：完整登录流程集成测试（三种方式）
- T9-2：权限隔离集成测试（跨租户访问拒绝）
- T9-3：Token 黑名单集成测试（禁用账户、单设备、租户停用）
- T9-4：性能测试（登录接口 P95 ≤ 300ms，网关 P95 ≤ 100ms）
- T9-5：安全扫描（dependency-check 0 High/Critical CVE）

---

## 依赖关系

```
T0 → T1 → T2 → T3 → T4 → T5 → T6
                              ↓
                    T7（并行于 T5/T6）
                    T8（依赖 T4 完成）
                              ↓
                             T9
```

---

## 关键技术决策参考

详见：`specs/001-multi-auth-tenant-rbac/research.md`

---

## 生成产物

| 文件 | 说明 |
|------|------|
| specs/001-multi-auth-tenant-rbac/spec.md | 功能规格 |
| specs/001-multi-auth-tenant-rbac/research.md | 技术研究决策 |
| specs/001-multi-auth-tenant-rbac/data-model.md | 数据模型 |
| specs/001-multi-auth-tenant-rbac/contracts/api-contracts.md | API 契约 |
| specs/001-multi-auth-tenant-rbac/checklists/requirements.md | 需求质量检查 |
| specs/001-multi-auth-tenant-rbac/checklists/full-review.md | 完整评审检查（50/50）|
