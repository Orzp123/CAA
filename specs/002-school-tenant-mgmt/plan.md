# 实现计划: feature/002 学校租户管理与账户管理

**日期**: 2026-06-25
**分支**: feature/002-school-tenant-mgmt
**规格**: [spec.md](spec.md)
**数据模型**: [data-model.md](data-model.md)
**API 契约**: [contracts/api.md](contracts/api.md)
**研究决策**: [research.md](research.md)

---

## 宪法符合性

| 原则 | 状态 | 说明 |
|------|------|------|
| Spec-First | ✅ | spec.md 完整，16/16 通过 |
| API-First | ✅ | REST API 契约已定义 |
| Multi-Tenant | ✅ | 所有新表含 tenant_id |
| Test-First | ✅ | 每阶段先写测试 |
| 禁止项 | ✅ | 无 Lombok/MyBatis/MongoDB |
| 性能设计 | ✅ | 分页/MinIO 直传/Virtual Threads |

---

## 实现阶段

### Phase 1：数据库迁移

**文件**: `backend/src/main/resources/db/migration/V2__school_tenant_mgmt.sql`

按 `data-model.md` V2 Migration SQL 执行：
- ALTER tenants（logo_url、system_name_zh/en、description）
- ALTER accounts（email、phone、secondary_role）
- CREATE benefit_packages、tenant_benefit_packages、promotional_slots
- INSERT 预置套餐（STANDARD、PRO）

**验收**: `mvn flyway:migrate` 无报错

---

### Phase 2：后端 school 包（TDD）

**包路径**: `com.caa.school`

```
com.caa.school/
├── controller/
│   ├── SchoolController.java
│   └── SchoolAccountController.java
├── service/
│   ├── SchoolService.java
│   ├── SchoolAccountService.java
│   └── BatchImportService.java
├── repository/
│   ├── BenefitPackageRepository.java
│   ├── TenantBenefitPackageRepository.java
│   └── PromotionalSlotRepository.java
├── model/
│   ├── BenefitPackage.java
│   ├── TenantBenefitPackage.java
│   └── PromotionalSlot.java
└── dto/
    ├── CreateSchoolRequest.java
    ├── UpdateSchoolRequest.java
    ├── SchoolResponse.java
    ├── CreateAccountRequest.java
    ├── UpdateAccountRequest.java
    ├── AccountResponse.java
    └── BatchImportResponse.java
```

**TDD 顺序**（先写测试再写实现）:
1. SchoolService — 新建学校（@Transactional 原子操作）
2. SchoolService — 学校列表分页搜索
3. SchoolService — 编辑学校、停用/启用
4. SchoolService — Logo 预签名 URL（MinIO mock）
5. SchoolAccountService — 创建账户（第二身份校验、email/phone 格式校验）
6. SchoolAccountService — 账户列表分页 AND 查询
7. SchoolAccountService — 编辑、重置密码、停用/启用、删除（含自身保护）
8. BatchImportService — 批量导入（幂等、部分成功、结果文件写 MinIO）
9. SchoolAccountService — 批量状态修改、批量删除（排除自身）

**关键约束**:
- 所有 Repository 查询携带 tenantId 过滤
- 批量导入使用 Virtual Threads
- 每个 Controller 方法必须有 `@Operation` + `@ApiResponse`

**验收**: `mvn test` — 核心模块覆盖率 ≥ 80%，0 failure

---

### Phase 3：前端（TDD）

**前置**: 所有页面先调用 `/frontend-design` 技能，用户确认后再实现

**页面**:
- `app/admin/schools/page.tsx` — 学校列表
- `app/admin/schools/[id]/page.tsx` — 新建/编辑学校
- `app/admin/schools/[id]/accounts/page.tsx` — 账户管理

**API 层**: `frontend/src/lib/schoolApi.ts`

**TDD 顺序**:
1. schoolApi.ts — 所有接口 mock 测试
2. 学校列表组件测试
3. 创建学校表单（含套餐自动勾选权限）
4. 账户列表与查询测试
5. 批量导入（文件上传、结果下载）

**验收**: `npm test -- --run` 全部通过

---

### Phase 4：质量门禁

| 检查项 | 命令 | 目标 |
|--------|------|------|
| 后端编译 | `mvn compile` | 0 error, 0 warning |
| 后端测试 | `mvn test` | ≥ 80%，0 failure |
| 前端编译 | `pnpm build` | 0 error |
| 前端测试 | `npm test -- --run` | 全部通过 |
| API 文档 | Swagger UI | 所有端点有注解 |

---

## 依赖关系

```
Phase 1（DB）→ Phase 2（后端）→ Phase 3（前端）→ Phase 4（质量门禁）
```

Phase 3 API 层可与 Phase 2 后期并行（mock 优先）。

---

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| V2 migration ALTER TABLE 耗时 | 开发环境无存量数据，可接受 |
| 批量导入 1000 条 ≤30s | Virtual Threads + saveAll 批量，估算 <10s |
| MinIO 预签名 URL 需基础设施 | 集成测试标注 @Disabled，单元测试 mock |
| school 包复用 auth 包实体 | Spring Bean 注入，不直接访问 auth 包内部类 |
