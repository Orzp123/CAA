# ADR-001: 多租户认证架构

**Date**: 2026-06-17
**Status**: Accepted
**Feature**: 001-multi-auth-tenant-rbac

---

## Context

CAA Phase 1 使用简单的本地 JWT 认证（TenantFilter 解析 X-Tenant-Id Header）。随着平台需要服务多所学校，需要统一认证入口、多登录方式和细粒度权限管理。

---

## Decisions

### 1. 统一认证网关：Spring Cloud Gateway（独立服务）

**决定**：新建独立 `gateway/` 目录（方案 A），使用 Spring Cloud Gateway 5.0.2（WebFlux）。

**理由**：WebFlux 响应式高并发，无状态支持 Phase 2 水平扩展，与 Spring Cloud 2025.1.2 原生集成。Backend 端口迁移 :8080 → :8081，Gateway :8080 成为唯一对外入口。

### 2. JWT：nimbus-jose-jwt（Spring Security 内置）

**决定**：`spring-boot-starter-oauth2-resource-server`，使用 `JwtEncoder`/`JwtDecoder` 接口抽象。

**理由**：无额外依赖，支持 HS256/RS256/ES256，配合 `@RefreshScope` 实现 Nacos 算法热切换。

### 3. 微信 OAuth2：自定义 `WechatOAuth2AccessTokenResponseClient`

**理由**：微信 token 响应中直接返回 openid，不符合 RFC 6749，需自定义客户端适配。

### 4. SSO：Spring Security OIDC + 动态 ClientRegistration

**决定**：`tenant_sso_configs` 表存储各校配置，启动时动态注册 `ClientRegistration`。

### 5. Token 黑名单：Redis JTI，TTL = Token 剩余有效期

**决定**：`token:blacklist:{jti}` Redis String，覆盖账户禁用/单设备/租户停用三场景，O(1) 查询。

### 6. 租户识别：Host Header > X-School-Code

**决定**：子域名 Host Header 优先，主域名通过前端 X-School-Code Header。

### 7. 验证码：kaptcha 2.3.2 + Redis

**决定**：`captcha:{uuid}` TTL 5min，一次性验证后立即删除。

### 8. Spring Cloud 版本兼容性

**决定**：BOM import 顺序覆盖：`spring-cloud-dependencies:2025.1.2`（先）> `spring-cloud-alibaba-dependencies:2025.1.0.0`（后）。

待 SCA issue #4339 合并后升级到官方支持版本并移除此覆盖。

---

## Consequences

- Bootstrap.yml 废弃，迁移至 `spring.config.import`
- 下游服务从 Authorization Header 解析 JWT，不再依赖 TenantFilter 的 X-Tenant-Id
- Backend 端口 :8080 → :8081（对外透明）
