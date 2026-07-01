# API Contracts: feature/002 学校租户管理与账户管理

**日期**: 2026-06-25
**Base URL**: `/api/v1`
**认证**: Bearer JWT（所有接口需认证）
**响应格式**: `ApiResponse<T>` — `{ "success": true, "data": T, "message": null }`
**错误格式**: `{ "success": false, "data": null, "message": "错误描述", "code": "ERROR_CODE" }`

---

## 系统管理员：学校管理

### POST /schools
新建学校（原子操作，同时创建默认学校管理员）

**权限**: SYSTEM_ADMIN

**请求体**:
```json
{
  "name": "测试大学",
  "code": "TEST",
  "domain": "test.edu.cn",
  "packageId": "20000000-0000-0000-0000-000000000001",
  "permissionCodes": ["AGENT_READ", "AGENT_WRITE"],
  "brand": {
    "systemNameZh": "测试大学教学平台",
    "systemNameEn": "TEST Teaching Platform",
    "description": "一段简介"
  },
  "slots": [
    {
      "title": "开学通知",
      "imageUrl": "schools/uuid/banner.jpg",
      "linkUrl": "/notice/1",
      "position": "HOME_TOP_BANNER",
      "sortOrder": 0
    }
  ]
}
```

**响应 201**:
```json
{
  "success": true,
  "data": {
    "schoolId": "uuid",
    "code": "TEST",
    "name": "测试大学",
    "defaultAdminLoginName": "admin_TEST",
    "status": "ACTIVE",
    "createdAt": "2026-06-25T10:00:00"
  }
}
```

**错误码**:
- `SCHOOL_CODE_DUPLICATE` — School Code 已存在（幂等：重复提交返回此错误）
- `PACKAGE_NOT_FOUND` — 套餐不存在
- `PERMISSION_NOT_FOUND` — 权限不存在

---

### GET /schools
学校列表（分页 + 搜索）

**权限**: SYSTEM_ADMIN

**Query 参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，从 0 开始，默认 0 |
| size | int | 每页条数，默认 20，最大 100 |
| name | string | 学校名称模糊搜索（可选） |
| code | string | 学校缩写模糊搜索（可选） |

**响应 200**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "code": "TEST",
        "name": "测试大学",
        "status": "ACTIVE",
        "adminCount": 2,
        "createdAt": "2026-06-25T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 20
  }
}
```

---

### GET /schools/{id}
学校详情（含品牌、套餐、权限、运营位）

**权限**: SYSTEM_ADMIN

**响应 200**: 完整学校对象，含 brand、package、permissionCodes、slots 字段

---

### PUT /schools/{id}
更新学校信息

**权限**: SYSTEM_ADMIN

**请求体**: 同 POST /schools（不含 code，code 创建后不可修改）

**响应 200**: 更新后的完整学校对象

---

### PATCH /schools/{id}/status
停用/启用学校

**权限**: SYSTEM_ADMIN

**请求体**: `{ "status": "INACTIVE" }`

**响应 200**: `{ "success": true, "data": { "id": "uuid", "status": "INACTIVE" } }`

---

### GET /schools/{id}/logo-upload-url
获取 Logo 上传预签名 URL（MinIO 直传）

**权限**: SYSTEM_ADMIN

**响应 200**:
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://minio.internal/presigned?token=xxx",
    "logoPath": "schools/uuid/logo.png",
    "expiresIn": 300
  }
}
```

---

## 学校管理员：账户管理

### POST /schools/{schoolId}/accounts
创建账户

**权限**: SCHOOL_ADMIN（仅可操作本校）

**请求体**:
```json
{
  "loginName": "s2026001",
  "name": "张三",
  "accountType": "STUDENT",
  "secondaryRole": null,
  "nickname": "小张",
  "email": "zhangsan@test.edu.cn",
  "phone": "13800138000",
  "password": "MyPassword123"
}
```
`password` 可不传，留空则使用平台默认密码策略生成。

**响应 201**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "loginName": "s2026001",
    "name": "张三",
    "accountType": "STUDENT",
    "secondaryRole": null,
    "status": "ACTIVE",
    "createdAt": "2026-06-25T10:00:00"
  }
}
```

**错误码**:
- `LOGIN_NAME_DUPLICATE` — 登录名在本租户内已存在
- `SECONDARY_ROLE_INVALID` — 第二身份与第一身份不兼容
- `EMAIL_FORMAT_INVALID` — 邮箱格式错误
- `PHONE_FORMAT_INVALID` — 手机号格式错误

---

### GET /schools/{schoolId}/accounts
账户列表（分页 + 多条件 AND 查询）

**权限**: SCHOOL_ADMIN

**Query 参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，默认 0 |
| size | int | 每页，默认 20，最大 100 |
| loginName | string | 模糊匹配（可选） |
| accountType | enum | SCHOOL_ADMIN/TEACHER/STUDENT（可选） |
| name | string | 模糊匹配（可选） |

多条件同时传入取交集（AND 逻辑）。

**响应 200**: 分页列表，含 loginName、name、accountType、secondaryRole、status、createdAt

---

### PUT /schools/{schoolId}/accounts/{accountId}
编辑账户

**权限**: SCHOOL_ADMIN

**请求体**:
```json
{
  "name": "张三",
  "nickname": "小张",
  "email": "new@test.edu.cn",
  "phone": "13900139000"
}
```
`accountType`、`secondaryRole` 不在此接口，不可修改。

---

### PATCH /schools/{schoolId}/accounts/{accountId}/password
重置密码

**权限**: SCHOOL_ADMIN

**请求体**: `{ "password": "NewPassword123" }`（可不传，留空使用默认策略）

---

### PATCH /schools/{schoolId}/accounts/{accountId}/status
启用/停用账户

**权限**: SCHOOL_ADMIN

**请求体**: `{ "status": "DISABLED" }`

---

### DELETE /schools/{schoolId}/accounts/{accountId}
删除账户（单条，需前端二次确认后调用）

**权限**: SCHOOL_ADMIN

**约束**: 不可删除操作者自身账户

**错误码**: `SELF_DELETE_FORBIDDEN`

---

### POST /schools/{schoolId}/accounts/batch-import
批量导入账户

**权限**: SCHOOL_ADMIN

**请求**: `multipart/form-data`，字段名 `file`，支持 `.xlsx` / `.csv`

**约束**: 有效数据行数（不含表头）≤ 1000；接口幂等，登录名已存在记入错误

**响应 200**:
```json
{
  "success": true,
  "data": {
    "total": 50,
    "successCount": 48,
    "failureCount": 2,
    "reportDownloadUrl": "https://minio.internal/presigned-download?token=xxx"
  }
}
```

报告文件（Excel）列：loginName、name、accountType、secondaryRole、password（系统生成密码）、result（SUCCESS/FAILURE）、failureReason

**错误码**:
- `FILE_TOO_LARGE` — 有效数据行超过 1000 条
- `FILE_FORMAT_INVALID` — 不支持的文件格式

---

### PATCH /schools/{schoolId}/accounts/batch-status
批量修改账户状态

**权限**: SCHOOL_ADMIN

**请求体**:
```json
{
  "accountIds": ["uuid1", "uuid2"],
  "status": "DISABLED"
}
```

---

### DELETE /schools/{schoolId}/accounts/batch
批量删除账户（需前端二次确认后调用）

**权限**: SCHOOL_ADMIN

**请求体**: `{ "accountIds": ["uuid1", "uuid2"] }`

**约束**: 自动排除操作者自身 ID

**响应 200**: `{ "success": true, "data": { "deletedCount": 2, "excludedSelfCount": 0 } }`

---

## 公共接口

### GET /benefit-packages
获取可用套餐列表（含默认权限 code）

**权限**: SYSTEM_ADMIN

### GET /permissions
获取平台权限列表（复用 feature/001 接口）

**权限**: SYSTEM_ADMIN
