# Tasks: 多租户用户认证与权限管理系统

**Feature**: 001-multi-auth-tenant-rbac
**Generated**: 2026-06-17
**Spec**: specs/001-multi-auth-tenant-rbac/spec.md
**Plan**: specs/001-multi-auth-tenant-rbac/plan.md

---

## 用户故事映射

| ID | 用户故事 | 优先级 |
|----|---------|--------|
| US1 | 管理员创建租户、配置登录方式和域名 | P1 |
| US2 | 学生/教师通过账号密码（学号+学校+密码+验证码）登录 | P1 |
| US3 | 用户通过微信 OAuth2 登录，首次登录补全信息 | P2 |
| US4 | 用户通过第三方 SSO（OIDC）登录，首次登录补全信息 | P2 |
| US5 | 总 ADMIN 配置租户权限和角色权限，变更登录后生效 | P1 |
| US6 | 网关统一拦截请求，Token 验证、权限校验、白名单放行 | P1 |
| US7 | 账号密码登录前端页面，支持多登录方式切换 | P2 |
| US8 | 权限管理前端页面，总 ADMIN 可视化配置 | P3 |

---

## Phase 1：基础设施准备

**目标**：数据库建表、Nacos 配置、Docker 服务、ADR

- [ ] T001 编写 ADR-001 认证架构决策文档 `docs/adr/001-auth-architecture.md`
- [ ] T002 编写 Flyway 数据库迁移脚本 `backend/src/main/resources/db/migration/V2__auth_schema.sql`（含 tenants / tenant_login_configs / tenant_sso_configs / accounts / permissions / tenant_permissions / role_permissions / system_configs / tenant_single_device_configs 共 9 张表）
- [ ] T003 [P] 初始化 Nacos 配置命名空间，添加配置项：jwt.algorithm=HS256、jwt.expiration_seconds=7200、login.max_fail_count=5、login.lock_duration_seconds=900、single_device.enabled=false、captcha.expiration_seconds=300、gateway.whitelist 列表
- [ ] T004 [P] Docker Compose 新增 gateway 服务配置 `docker-compose.yml`，端口 8080，依赖 nacos/redis/backend
- [ ] T005 [P] 创建 gateway 模块目录结构 `gateway/src/main/java/com/caa/gateway/`，初始化 pom.xml（spring-cloud-gateway-server、spring-cloud-starter-alibaba-nacos-discovery、spring-cloud-starter-alibaba-nacos-config、spring-data-redis-reactive）
- [ ] T006 [P] 在 backend 模块创建 auth 包结构 `backend/src/main/java/com/caa/auth/`（model / dto / repository / service / controller / wechat 子目录）

---

## Phase 2：基础实体层（阻塞前提）

**目标**：所有实体和 Repository 完成，后续阶段均依赖此层
**独立验收**：所有 Repository 单元测试通过，覆盖率 ≥ 80%

- [ ] T007 [TDD] 编写 Tenant 实体测试 `backend/src/test/java/com/caa/auth/model/TenantTest.java`
- [ ] T008 编写 Tenant 实体 `backend/src/main/java/com/caa/auth/model/Tenant.java`（含状态枚举 TenantStatus、类型枚举 TenantType）
- [ ] T009 [TDD] 编写 TenantRepository 测试 `backend/src/test/java/com/caa/auth/repository/TenantRepositoryTest.java`（域名查询、code 查询、状态过滤）
- [ ] T010 编写 TenantRepository `backend/src/main/java/com/caa/auth/repository/TenantRepository.java`
- [ ] T011 [P][TDD] 编写 Account 实体测试 `backend/src/test/java/com/caa/auth/model/AccountTest.java`
- [ ] T012 [P] 编写 Account 实体 `backend/src/main/java/com/caa/auth/model/Account.java`（含 AccountType / AccountStatus 枚举）
- [ ] T013 [P][TDD] 编写 AccountRepository 测试 `backend/src/test/java/com/caa/auth/repository/AccountRepositoryTest.java`（学号+租户唯一查询、微信 openid 查询、锁定状态查询）
- [ ] T014 [P] 编写 AccountRepository `backend/src/main/java/com/caa/auth/repository/AccountRepository.java`
- [ ] T015 [P][TDD] 编写 Permission / TenantPermission / RolePermission 实体测试 `backend/src/test/java/com/caa/auth/model/PermissionTest.java`
- [ ] T016 [P] 编写 Permission 实体 `backend/src/main/java/com/caa/auth/model/Permission.java`
- [ ] T017 [P] 编写 TenantPermission 实体 `backend/src/main/java/com/caa/auth/model/TenantPermission.java`
- [ ] T018 [P] 编写 RolePermission 实体 `backend/src/main/java/com/caa/auth/model/RolePermission.java`
- [ ] T019 [P] 编写 PermissionRepository `backend/src/main/java/com/caa/auth/repository/PermissionRepository.java`
- [ ] T020 [P] 编写 TenantPermissionRepository `backend/src/main/java/com/caa/auth/repository/TenantPermissionRepository.java`
- [ ] T021 [P] 编写 RolePermissionRepository `backend/src/main/java/com/caa/auth/repository/RolePermissionRepository.java`
- [ ] T022 [P] 编写 TenantLoginConfig 实体和 Repository `backend/src/main/java/com/caa/auth/model/TenantLoginConfig.java` + `repository/TenantLoginConfigRepository.java`
- [ ] T023 [P] 编写 TenantSsoConfig 实体和 Repository `backend/src/main/java/com/caa/auth/model/TenantSsoConfig.java` + `repository/TenantSsoConfigRepository.java`
- [ ] T024 [P] 编写 SystemConfig / TenantSingleDeviceConfig 实体和 Repository `backend/src/main/java/com/caa/auth/model/SystemConfig.java` + `TenantSingleDeviceConfig.java` + 对应 Repository
- [ ] T025 [P] 编写所有 Record DTO `backend/src/main/java/com/caa/auth/dto/`（LoginRequest / LoginResponse / TokenInfo / WechatCallbackRequest / SsoCallbackRequest / ProfileCompletionRequest）

---

## Phase 3：Token 服务（US6 前置）

**目标**：JWT 签发/验证/黑名单/热加载，单设备限制核心逻辑
**独立验收**：TokenService 单元测试通过；Redis 黑名单 put/check/expire 逻辑正确

- [ ] T026 [TDD] 编写 TokenService 测试 `backend/src/test/java/com/caa/auth/service/TokenServiceTest.java`（签发、解析、过期验证、JTI 唯一性）
- [ ] T027 编写 TokenService `backend/src/main/java/com/caa/auth/service/TokenService.java`（nimbus-jose-jwt，@RefreshScope，Nacos 热加载算法/过期时间）
- [ ] T028 [TDD] 编写 TokenBlacklistService 测试 `backend/src/test/java/com/caa/auth/service/TokenBlacklistServiceTest.java`
- [ ] T029 编写 TokenBlacklistService `backend/src/main/java/com/caa/auth/service/TokenBlacklistService.java`（Redis key: token:blacklist:{jti}，TTL = 剩余有效期）
- [ ] T030 [TDD] 编写 SingleDeviceService 测试 `backend/src/test/java/com/caa/auth/service/SingleDeviceServiceTest.java`（三层配置合并：账户类型 > 租户 > 全局）
- [ ] T031 编写 SingleDeviceService `backend/src/main/java/com/caa/auth/service/SingleDeviceService.java`（Redis key: token:active:{accountId}:{deviceType}，旧 Token 加黑名单）

---

## Phase 4：US1 — 租户管理

**目标**：总 ADMIN 创建/配置学校租户，含登录方式、域名、SSO 配置
**独立验收**：调用 `POST /admin/tenants` 可创建租户；租户域名查询返回正确配置；停用租户后账户无法登录

- [ ] T032 [TDD][US1] 编写 TenantService 测试 `backend/src/test/java/com/caa/auth/service/TenantServiceTest.java`（CRUD、停用时 Token 处理、至少一种登录方式校验）
- [ ] T033 [US1] 编写 TenantService `backend/src/main/java/com/caa/auth/service/TenantService.java`（含停用时批量 Token 加黑名单逻辑、Redis 缓存 tenant:domain:{domain}）
- [ ] T034 [TDD][US1] 编写 TenantController 测试 `backend/src/test/java/com/caa/auth/controller/TenantControllerTest.java`
- [ ] T035 [US1] 编写 TenantController `backend/src/main/java/com/caa/auth/controller/TenantController.java`（@Operation/@ApiResponse 注解完整，SYSTEM_ADMIN 权限校验）
- [ ] T036 [P][US1] 编写 GET /auth/tenant-config 端点（登录页初始化接口）在 `AuthController.java` 中（返回租户登录方式配置，Redis 缓存 tenant:info:{tenantId}）
- [ ] T037 [P][US1] 编写租户 SSO 配置接口 PUT /admin/tenants/{tenantId}/sso-config `TenantController.java`（clientSecret 加密存储）
- [ ] T038 [P][US1] 编写数据库初始化脚本插入系统预置功能模块数据 `backend/src/main/resources/db/migration/V3__default_permissions.sql`

---

## Phase 5：US2 — 账号密码登录

**目标**：学号+学校+密码+验证码登录，失败锁定，Token 签发
**独立验收**：正确密码+验证码返回 Token；错误密码 5 次后账户锁定 15 分钟；Token 在网关可验证

- [ ] T039 [TDD][US2] 编写 CaptchaService 测试 `backend/src/test/java/com/caa/auth/service/CaptchaServiceTest.java`（生成、验证、一次性使用、TTL）
- [ ] T040 [US2] 编写 CaptchaService `backend/src/main/java/com/caa/auth/service/CaptchaService.java`（kaptcha 库，Redis key: captcha:{uuid}，TTL 可配）
- [ ] T041 [TDD][US2] 编写 PasswordLoginService 测试 `backend/src/test/java/com/caa/auth/service/PasswordLoginServiceTest.java`（密码验证、失败计数、锁定逻辑、单设备处理）
- [ ] T042 [US2] 编写 PasswordLoginService `backend/src/main/java/com/caa/auth/service/PasswordLoginService.java`（BCrypt 密码验证，失败计数 Nacos 可配，锁定时间可配）
- [ ] T043 [TDD][US2] 编写 AuthService 测试 `backend/src/test/java/com/caa/auth/service/AuthServiceTest.java`（整合登录流程、Token 签发）
- [ ] T044 [US2] 编写 AuthService `backend/src/main/java/com/caa/auth/service/AuthService.java`（路由三种登录方式，Token 签发，单设备处理）
- [ ] T045 [TDD][US2] 编写 AuthController 测试 `backend/src/test/java/com/caa/auth/controller/AuthControllerTest.java`（POST /auth/login、GET /auth/captcha）
- [ ] T046 [US2] 编写 AuthController `backend/src/main/java/com/caa/auth/controller/AuthController.java`（POST /auth/login、GET /auth/captcha、POST /auth/logout，@Operation/@ApiResponse 完整）

---

## Phase 6：US5 — 权限管理

**目标**：总 ADMIN 配置租户权限和角色权限，Redis 缓存，变更登录后生效
**独立验收**：配置租户权限后，该租户账户重新登录时新权限生效；角色权限配置正确映射

- [ ] T047 [TDD][US5] 编写 PermissionService 测试 `backend/src/test/java/com/caa/auth/service/PermissionServiceTest.java`（CRUD、系统内置不可删除）
- [ ] T048 [US5] 编写 PermissionService `backend/src/main/java/com/caa/auth/service/PermissionService.java`
- [ ] T049 [TDD][US5] 编写 TenantPermissionService 测试 `backend/src/test/java/com/caa/auth/service/TenantPermissionServiceTest.java`（缓存失效：permission:tenant:{tenantId}）
- [ ] T050 [US5] 编写 TenantPermissionService `backend/src/main/java/com/caa/auth/service/TenantPermissionService.java`（Redis Set 缓存，变更时 evict）
- [ ] T051 [P][TDD][US5] 编写 RolePermissionService 测试 `backend/src/test/java/com/caa/auth/service/RolePermissionServiceTest.java`（缓存失效：permission:role:{tenantId}:{roleType}）
- [ ] T052 [P][US5] 编写 RolePermissionService `backend/src/main/java/com/caa/auth/service/RolePermissionService.java`
- [ ] T053 [TDD][US5] 编写 PermissionController 测试 `backend/src/test/java/com/caa/auth/controller/PermissionControllerTest.java`
- [ ] T054 [US5] 编写 PermissionController `backend/src/main/java/com/caa/auth/controller/PermissionController.java`（GET/POST /admin/permissions，PUT /admin/tenants/{id}/permissions，PUT /admin/tenants/{id}/role-permissions/{type}，@Operation/@ApiResponse 完整）

---

## Phase 7：US6 — 认证网关

**目标**：Spring Cloud Gateway，Token 验证，权限校验，租户识别，白名单热加载
**独立验收**：无 Token 请求返回 401；Token 有效但无权限返回 403；白名单路径无需 Token；Host Header 正确识别租户

- [ ] T055 [TDD][US6] 编写 TenantResolutionFilter 测试 `gateway/src/test/java/com/caa/gateway/filter/TenantResolutionFilterTest.java`（子域名识别、X-School-Code 识别、Redis 缓存）
- [ ] T056 [US6] 编写 TenantResolutionFilter `gateway/src/main/java/com/caa/gateway/filter/TenantResolutionFilter.java`（WebFlux，Host Header 解析，Redis 缓存 tenant:domain:{domain}）
- [ ] T057 [TDD][US6] 编写 AuthenticationFilter 测试 `gateway/src/test/java/com/caa/gateway/filter/AuthenticationFilterTest.java`（Token 验证、黑名单查询、权限粗校验、401/403 响应）
- [ ] T058 [US6] 编写 AuthenticationFilter `gateway/src/main/java/com/caa/gateway/filter/AuthenticationFilter.java`（WebFlux，JWT 解析，Redis 黑名单查询，租户级权限校验）
- [ ] T059 [US6] 编写 WhitelistConfig `gateway/src/main/java/com/caa/gateway/config/WhitelistConfig.java`（@RefreshScope，Nacos 热加载白名单路径列表）
- [ ] T060 [US6] 编写 GatewayRouteConfig `gateway/src/main/java/com/caa/gateway/config/GatewayRouteConfig.java`（路由规则，Nacos 服务发现，下游服务路由）
- [ ] T061 [US6] 编写 gateway bootstrap.yml `gateway/src/main/resources/bootstrap.yml`（Nacos 注册、配置中心地址）

---

## Phase 8：US3 — 微信登录

**目标**：微信 OAuth2 授权码流程，首次登录补全信息，归属微信大学租户
**独立验收**：微信回调后系统创建账户归属微信大学；放弃补全时账户不保存；再次微信登录复用已有账户

- [ ] T062 [TDD][US3] 编写 WechatOAuth2Service 测试 `backend/src/test/java/com/caa/auth/service/WechatOAuth2ServiceTest.java`（access_token 获取、openid 解析、首次登录流程、放弃补全处理）
- [ ] T063 [US3] 编写 WechatOAuth2Service `backend/src/main/java/com/caa/auth/service/wechat/WechatOAuth2Service.java`（自定义 OAuth2AccessTokenResponseClient，微信 userinfo 接口，tempToken Redis 存储）
- [ ] T064 [US3] 在 AuthController 添加微信相关端点 `backend/src/main/java/com/caa/auth/controller/AuthController.java`（GET /auth/wechat/authorize、GET /auth/wechat/callback、POST /auth/wechat/complete-profile）

---

## Phase 9：US4 — 第三方 SSO 登录

**目标**：OAuth2/OIDC 标准流程，动态 ClientRegistration，IdP claim 映射，首次登录补全
**独立验收**：不同学校 SSO 回调正确映射对应租户；IdP role claim 正确转换账户类型；识别失败默认学生

- [ ] T065 [TDD][US4] 编写 SsoOidcService 测试 `backend/src/test/java/com/caa/auth/service/SsoOidcServiceTest.java`（动态 ClientRegistration、claim 映射、默认学生逻辑）
- [ ] T066 [US4] 编写 SsoOidcService `backend/src/main/java/com/caa/auth/service/SsoOidcService.java`（InMemoryClientRegistrationRepository 动态注册，OIDC Discovery，role claim 映射）
- [ ] T067 [US4] 在 AuthController 添加 SSO 相关端点 `backend/src/main/java/com/caa/auth/controller/AuthController.java`（GET /auth/sso/{tenantCode}/authorize、GET /auth/sso/{tenantCode}/callback、POST /auth/sso/complete-profile）

---

## Phase 10：US7 — 登录前端页面

**目标**：多登录方式切换登录页，验证码，Token 存储，过期提示
**独立验收**：账号密码登录成功跳转工作台；验证码点击刷新；Token 过期提示 3 秒后跳转登录页；不支持的登录方式入口不显示

- [ ] T068 [US7] 调用 `/frontend-design` 技能设计登录页 UI 方案（多登录方式切换、验证码、学校选择器）
- [ ] T069 [TDD][US7] 编写 authApi.ts 测试 `frontend/src/lib/__tests__/authApi.test.ts`（login、logout、getTenantConfig 接口）
- [ ] T070 [US7] 编写 authApi.ts `frontend/src/lib/authApi.ts`（封装 /auth/* 接口，含验证码获取）
- [ ] T071 [US7] 编写 useAuth Zustand store `frontend/src/lib/stores/authStore.ts`（Token 存储、过期检测、单设备踢出提示）
- [ ] T072 [US7] 编写 PasswordLoginForm 组件 `frontend/src/components/auth/PasswordLoginForm.tsx`（学号/学校/密码/验证码，点击图片刷新验证码）
- [ ] T073 [P][US7] 编写 WechatLoginButton 组件 `frontend/src/components/auth/WechatLoginButton.tsx`（跳转微信授权）
- [ ] T074 [P][US7] 编写 SsoLoginButton 组件 `frontend/src/components/auth/SsoLoginButton.tsx`（跳转 SSO）
- [ ] T075 [US7] 编写 LoginPage `frontend/src/app/login/page.tsx`（多登录方式切换，条件渲染，学校选择器，初始化 getTenantConfig）

---

## Phase 11：US8 — 权限管理前端页面

**目标**：总 ADMIN 可视化配置租户权限、角色权限、单设备限制
**独立验收**：总 ADMIN 修改租户权限后保存成功；学校 ADMIN 无法访问权限管理页面（403）

- [ ] T076 [US8] 调用 `/frontend-design` 技能设计权限管理页 UI 方案（租户权限/角色权限/单设备配置）
- [ ] T077 [US8] 编写 permissionApi.ts `frontend/src/lib/permissionApi.ts`（封装权限相关 API）
- [ ] T078 [TDD][US8] 编写 TenantPermissionPage 测试 `frontend/src/app/admin/permissions/__tests__/TenantPermissionPage.test.tsx`
- [ ] T079 [US8] 编写 TenantPermissionPage `frontend/src/app/admin/permissions/tenant/page.tsx`（租户功能模块权限配置，仅总 ADMIN 可见）
- [ ] T080 [P][US8] 编写 RolePermissionPage `frontend/src/app/admin/permissions/role/page.tsx`（角色操作权限配置）
- [ ] T081 [P][US8] 编写 SingleDeviceConfigPage `frontend/src/app/admin/permissions/single-device/page.tsx`（三层单设备限制配置）

---

## Phase 12：集成测试与质量门禁

**目标**：端到端集成测试，性能测试，安全扫描

- [ ] T082 编写三种登录方式完整流程集成测试 `backend/src/test/java/com/caa/auth/integration/AuthIntegrationTest.java`
- [ ] T083 [P] 编写权限隔离集成测试 `backend/src/test/java/com/caa/auth/integration/PermissionIsolationTest.java`（跨租户访问拒绝，角色权限校验）
- [ ] T084 [P] 编写 Token 黑名单集成测试 `backend/src/test/java/com/caa/auth/integration/TokenBlacklistIntegrationTest.java`（禁用账户、单设备踢出、租户停用）
- [ ] T085 [P] 编写网关集成测试 `gateway/src/test/java/com/caa/gateway/GatewayIntegrationTest.java`（白名单放行、401/403 响应格式）
- [ ] T086 编写登录接口性能测试脚本 `tests/performance/auth-load-test.js`（k6，目标：P95 ≤ 300ms，并发 500）
- [ ] T087 [P] 运行 dependency-check 安全扫描，确认 0 High/Critical CVE
- [ ] T088 [P] 验证 Swagger UI 所有 auth/tenant/permission 端点注解完整

---

## 依赖关系

```
Phase 1（T001-T006）
  └── Phase 2（T007-T025）
        ├── Phase 3（T026-T031）Token 服务
        │     ├── Phase 4（T032-T038）US1 租户管理
        │     ├── Phase 5（T039-T046）US2 密码登录
        │     ├── Phase 6（T047-T054）US5 权限管理
        │     └── Phase 7（T055-T061）US6 网关 ← 依赖 Phase 3+4+5+6
        │
        ├── Phase 8（T062-T064）US3 微信登录 ← 依赖 Phase 5
        ├── Phase 9（T065-T067）US4 SSO 登录 ← 依赖 Phase 5
        ├── Phase 10（T068-T075）US7 前端登录 ← 依赖 Phase 5
        └── Phase 11（T076-T081）US8 前端权限 ← 依赖 Phase 6
              └── Phase 12（T082-T088）集成测试 ← 依赖所有
```

---

## 并行执行机会

**Phase 2 内部可并行**（T011-T025 均标注 [P]）：
- Account 实体组（T011-T014）
- Permission 实体组（T015-T021）
- Config 实体组（T022-T025）

**Phase 4-7 可并行**（均依赖 Phase 3 完成）：
- US1 租户管理（T032-T038）
- US2 密码登录（T039-T046）
- US5 权限管理（T047-T054）

**Phase 8、9、10、11 可并行**（依赖各自前置完成）

---

## MVP 范围建议

最小可验证版本 = **Phase 1 + Phase 2 + Phase 3 + Phase 5 + Phase 7 + Phase 10**（T001-T061，US2 账号密码登录 + 网关验证）

完整版本按 US3 → US4 → US8 顺序递增交付。
