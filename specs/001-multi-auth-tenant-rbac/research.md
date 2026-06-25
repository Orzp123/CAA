# Research: 多租户用户认证与权限管理系统

**Created**: 2026-06-17
**Feature**: 001-multi-auth-tenant-rbac

---

## 决策 1：网关实现方案

**决策**：使用 Spring Cloud Gateway（独立服务）作为统一认证网关

**理由**：
- Spring Boot 4.x 生态原生支持，与 Nacos 服务注册、Spring Security 无缝集成
- 支持路由过滤器（GatewayFilter）实现 Token 验证、权限校验、白名单放行
- 响应式（WebFlux）架构，支持高并发连接，满足宪法 ≥500 并发用户要求
- 无状态设计天然支持 Phase 2 水平扩展

**替代方案**：
- Nginx Lua 脚本：实现复杂，调试困难，与 Java 生态割裂
- Spring MVC Filter：不支持动态路由，扩展性差

---

## 决策 2：JWT 库选型

**决策**：使用 `nimbus-jose-jwt`（Spring Security OAuth2 内置依赖）

**理由**：
- Spring Security 6.x / Spring Boot 4.x 内置依赖，无需额外引入
- 支持 Spring Security 封装的全部算法：HS256/HS384/HS512（对称）、RS256/RS384/RS512（非对称）、ES256/ES384/ES512（椭圆曲线）
- 通过 `JwtDecoder` / `JwtEncoder` 接口抽象，切换算法只需更换 Bean 配置
- Nacos 热加载：`@RefreshScope` + `NacosConfigAutoRefresher` 实现算法配置热切换

**替代方案**：
- jjwt（io.jsonwebtoken）：需额外依赖，与 Spring Security 集成需适配层
- auth0 java-jwt：同上，增加依赖复杂度

---

## 决策 3：微信 OAuth2 集成

**决策**：使用 Spring Security OAuth2 Client 扩展 + 自定义 `OAuth2UserService`

**理由**：
- 微信网页授权遵循 OAuth2 授权码流程（code → access_token → openid/userinfo）
- Spring Security OAuth2 Client 提供标准流程，自定义 `WeChatOAuth2UserService` 处理微信特有字段（openid、unionid）
- 微信 AppID/AppSecret 通过 Nacos 配置中心管理，支持热加载
- 回调 URL 格式：`/auth/wechat/callback`

**关键差异处理**：
- 微信 access_token 接口与标准 OAuth2 不同，需自定义 `OAuth2AccessTokenResponseClient`
- 微信用户信息接口需携带 access_token + openid，需自定义 UserInfo 端点请求

---

## 决策 4：第三方 SSO（OAuth2/OIDC）集成

**决策**：使用 Spring Security OAuth2 Client，支持标准 OIDC Discovery

**理由**：
- Spring Security 原生支持 OIDC 1.0，自动从 `.well-known/openid-configuration` 发现端点
- 每个学校租户对应一个 `ClientRegistration`，动态注册到 `InMemoryClientRegistrationRepository`（支持 Nacos 热加载新增学校 SSO 配置）
- IdP 返回的 claims 中需包含账户类型字段（如 `edu_role`），映射规则可配置

**动态 ClientRegistration 方案**：
- 学校 SSO 配置存储在 MySQL `tenant_sso_config` 表
- 网关启动时加载所有启用的 SSO 配置，Nacos 事件触发动态刷新

---

## 决策 5：Token 黑名单（禁用账户、单设备限制、租户停用）

**决策**：使用 Redis Set 存储失效 Token 的 JTI（JWT ID）

**理由**：
- 固定过期 Token 无法主动撤销，需黑名单机制支持以下场景：
  1. 账户被禁用 → 立即失效所有 Token
  2. 单设备限制 → 新设备登录后旧设备 Token 加入黑名单
  3. 租户停用 → 该租户所有在线 Token 加入黑名单
- Redis TTL 设置与 Token 过期时间一致，自动清理过期条目
- 网关每次验证 Token 时查询 Redis 黑名单（O(1) 时间复杂度）
- Key 设计：`token:blacklist:{jti}` TTL = Token 剩余有效期

**替代方案**：
- 数据库存储黑名单：延迟高，不满足网关高并发要求
- 版本号（account_version）：需 Token 携带版本号，账户禁用时递增版本，但无法支持单设备粒度失效

---

## 决策 6：多域名租户识别

**决策**：Spring Cloud Gateway `Host` Header 路由断言 + 主域名学校选择器

**理由**：
- Gateway 路由规则：`Host: {tenantCode}.caa.example.com` → 提取 tenantCode → 查询 Redis 缓存租户信息
- 主域名访问：登录页通过学校选择器下拉框选择，前端携带 `X-School-Code` Header
- 租户-域名映射缓存：`tenant:domain:{domain}` → tenantId，TTL 5 分钟，Nacos 配置变更时主动失效

---

## 决策 7：单设备限制的在线 Token 追踪

**决策**：Redis Hash 存储账户活跃 Token

**理由**：
- Key：`token:active:{accountId}:{deviceType}` → JTI
- 新登录时：SET 新 JTI，将旧 JTI 加入黑名单
- 三层配置（全局/租户/账户类型）优先级：账户类型级 > 租户级 > 全局，存储在 Redis Hash：`singledevice:config` 热加载

---

## 决策 8：验证码实现

**决策**：使用 `kaptcha` 库生成图形验证码，存储至 Redis

**理由**：
- kaptcha（Google）：轻量、无额外服务依赖、Spring Boot 集成简单
- 验证码存储：`captcha:{uuid}` → code，TTL 5 分钟，一次性使用（验证后立即删除）
- 前端通过 `GET /auth/captcha?uuid={uuid}` 获取图片，提交时携带 uuid + code

---

## 决策 9：权限校验架构

**决策**：网关层粗粒度校验（租户级功能模块） + 服务层细粒度校验（角色级操作权限）

**理由**：
- 网关：验证 Token 有效性 + 检查租户是否有权访问该路径对应的功能模块
- 服务层：使用 Spring Security `@PreAuthorize` 注解校验角色级操作权限
- 权限数据缓存：`permission:tenant:{tenantId}` → 功能模块列表；`permission:role:{tenantId}:{roleType}` → 操作权限列表
- Token Payload 携带：accountId、tenantId、roleType（不携带完整权限列表，网关查缓存）

---

## 技术约束确认

- ✅ 无 Lombok、无 MyBatis
- ✅ Record DTO、sealed interface
- ✅ Virtual Threads 处理阻塞 I/O（验证码生成、数据库查询）
- ✅ Nacos 配置中心管理所有可热加载配置
- ✅ SpringDoc OpenAPI 注解完整
- ✅ 所有表含 tenantId
