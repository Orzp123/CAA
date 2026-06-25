# API Contracts: 多租户用户认证与权限管理系统

**Created**: 2026-06-17
**Feature**: 001-multi-auth-tenant-rbac
**Base Path**: `/auth`, `/admin/tenants`, `/admin/permissions`

---

## 认证接口

### POST /auth/login
账号密码登录

**Request**:
```json
{
  "studentNo": "2024001",
  "schoolCode": "school-a",
  "password": "Pass1234",
  "captchaUuid": "uuid-xxx",
  "captchaCode": "A3K9"
}
```

**Response 200**:
```json
{
  "code": 0,
  "data": {
    "token": "eyJ...",
    "expiresAt": "2026-06-17T12:00:00Z",
    "accountId": "uuid",
    "nickname": "张三",
    "accountType": "STUDENT",
    "tenantId": "uuid",
    "tenantName": "北京大学"
  }
}
```

**Errors**:
- `401` INVALID_CREDENTIALS：学号/密码错误
- `401` CAPTCHA_INVALID：验证码错误或过期
- `423` ACCOUNT_LOCKED：账户已锁定，含 `lockedUntil` 字段
- `403` ACCOUNT_DISABLED：账户已被禁用

---

### GET /auth/captcha
获取图形验证码

**Query**: `uuid` (String, 前端生成的唯一标识)

**Response 200**: `image/png` 图片流

---

### GET /auth/wechat/authorize
发起微信 OAuth2 授权

**Query**: `tenantCode` (可选，主域名访问时传入)

**Response 302**: 重定向至微信授权页

---

### GET /auth/wechat/callback
微信 OAuth2 回调

**Query**: `code`, `state`

**Response 200**（已有账户）: 同 `/auth/login` 200
**Response 200**（首次登录，需补全信息）:
```json
{
  "code": 0,
  "data": {
    "requiresProfileCompletion": true,
    "tempToken": "temp-xxx",
    "wechatNickname": "微信昵称"
  }
}
```

---

### POST /auth/wechat/complete-profile
微信首次登录补全必填信息

**Request**:
```json
{
  "tempToken": "temp-xxx",
  "studentNo": "2024001",
  "name": "张三",
  "nickname": "小张",
  "accountType": "STUDENT"
}
```

**Response 200**: 同 `/auth/login` 200

---

### GET /auth/sso/{tenantCode}/authorize
发起 SSO OAuth2/OIDC 授权

**Response 302**: 重定向至 IdP 授权页

---

### GET /auth/sso/{tenantCode}/callback
SSO 回调处理

**Query**: `code`, `state`

**Response 200**（已有账户）: 同 `/auth/login` 200
**Response 200**（首次登录）: 同微信首次登录响应，含 `requiresProfileCompletion: true`

---

### POST /auth/sso/complete-profile
SSO 首次登录补全必填信息

**Request**: 同微信补全接口

**Response 200**: 同 `/auth/login` 200

---

### POST /auth/logout
登出（将当前 Token JTI 加入黑名单）

**Header**: `Authorization: Bearer {token}`

**Response 200**:
```json
{ "code": 0, "data": null }
```

---

### GET /auth/tenant-config
获取租户登录配置（登录页初始化使用）

**Query**: `domain` 或 `tenantCode`

**Response 200**:
```json
{
  "code": 0,
  "data": {
    "tenantId": "uuid",
    "tenantName": "北京大学",
    "defaultLoginType": "PASSWORD",
    "availableLoginTypes": ["PASSWORD", "SSO"],
    "logoUrl": null
  }
}
```

---

## 租户管理接口（仅总 ADMIN）

### GET /admin/tenants
列表查询，支持分页

**Query**: `page`, `size`, `status`, `type`

**Response 200**:
```json
{
  "code": 0,
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

---

### POST /admin/tenants
创建学校租户

**Request**:
```json
{
  "code": "pku",
  "name": "北京大学",
  "domain": "pku.caa.example.com",
  "defaultLoginType": "PASSWORD",
  "availableLoginTypes": ["PASSWORD", "SSO"]
}
```

**Response 201**: 含完整租户对象

---

### PUT /admin/tenants/{tenantId}
更新租户信息

### PUT /admin/tenants/{tenantId}/status
启用/停用租户

**Request**:
```json
{ "status": "INACTIVE" }
```

**Response 200**: 停用时所有在线 Token 自然过期，触发后台任务

---

### PUT /admin/tenants/{tenantId}/sso-config
配置租户 SSO

**Request**:
```json
{
  "issuerUri": "https://sso.pku.edu.cn/.well-known/openid-configuration",
  "clientId": "caa-client",
  "clientSecret": "secret",
  "scope": "openid profile email",
  "roleClaim": "edu_role",
  "roleMapping": {
    "teacher": "TEACHER",
    "student": "STUDENT"
  }
}
```

---

## 权限管理接口（仅总 ADMIN）

### GET /admin/permissions
获取全部权限/功能模块列表

### POST /admin/permissions
新增权限

**Request**:
```json
{
  "code": "AGENT_MANAGE",
  "name": "Agent 管理",
  "module": "AGENT",
  "action": "WRITE"
}
```

---

### GET /admin/tenants/{tenantId}/permissions
获取租户权限配置

### PUT /admin/tenants/{tenantId}/permissions
更新租户权限配置（全量替换该租户的权限列表）

**Request**:
```json
{
  "permissionIds": ["uuid-1", "uuid-2"]
}
```

---

### GET /admin/tenants/{tenantId}/role-permissions/{accountType}
获取某租户某角色的权限配置

### PUT /admin/tenants/{tenantId}/role-permissions/{accountType}
更新角色权限配置

---

### GET /admin/tenants/{tenantId}/single-device-config
获取单设备限制配置（含三层继承结果）

### PUT /admin/tenants/{tenantId}/single-device-config
更新租户级单设备配置

**Request**:
```json
{
  "accountTypeConfigs": [
    { "accountType": "STUDENT", "enabled": true },
    { "accountType": "TEACHER", "enabled": false }
  ]
}
```

---

## 网关错误响应格式

所有经网关拦截的错误统一格式：

**401 未认证**:
```json
{
  "code": 401,
  "message": "未认证：Token 无效或已过期",
  "timestamp": "2026-06-17T10:00:00Z"
}
```

**403 无权限**:
```json
{
  "code": 403,
  "message": "无权限访问该资源",
  "timestamp": "2026-06-17T10:00:00Z"
}
```

---

## Token Payload（JWT Claims）

```json
{
  "jti": "uuid",
  "sub": "accountId",
  "tenantId": "uuid",
  "accountType": "STUDENT",
  "studentNo": "2024001",
  "iat": 1718000000,
  "exp": 1718007200
}
```
