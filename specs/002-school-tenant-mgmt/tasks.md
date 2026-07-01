# Tasks: feature/002 学校租户管理与账户管理

**日期**: 2026-06-25
**分支**: feature/002-school-tenant-mgmt
**计划**: [plan.md](plan.md)
**规格**: [spec.md](spec.md)

---

## 用户故事映射

| US | 需求 | 优先级 | FR |
|----|------|--------|-----|
| US1 | 系统管理员新建学校（含品牌/套餐/权限/运营位） | P0 | FR-01,02,12,13,14,15 |
| US2 | 系统管理员查看学校列表、编辑学校、停用/启用 | P0 | FR-03,04 |
| US3 | 学校管理员创建账户（含第二身份、密码策略） | P0 | FR-05 |
| US4 | 账户列表多条件 AND 查询 | P0 | FR-06 |
| US5 | 编辑账户、重置密码、停用/启用、删除账户 | P0 | FR-07,08 |
| US6 | 批量导入账户（幂等、部分成功、下载报告） | P1 | FR-09 |
| US7 | 批量修改账户状态、批量删除（排除自身） | P1 | FR-10,11 |

---

## Phase 1：Setup — 数据库迁移

- [ ] T001 创建 V2 迁移文件，按 data-model.md 执行全部 DDL（ALTER tenants/accounts，CREATE benefit_packages/tenant_benefit_packages/promotional_slots，INSERT 预置套餐）`backend/src/main/resources/db/migration/V2__school_tenant_mgmt.sql`

---

## Phase 2：Foundational — 后端基础设施

- [ ] T002 [P] 扩展 Tenant 实体，新增 logoUrl/systemNameZh/systemNameEn/description 字段 `backend/src/main/java/com/caa/auth/model/Tenant.java`
- [ ] T003 [P] 扩展 Account 实体，新增 email/phone/secondaryRole 字段及 SecondaryRole 枚举 `backend/src/main/java/com/caa/auth/model/Account.java`
- [ ] T004 [P] 创建 BenefitPackage JPA 实体 `backend/src/main/java/com/caa/school/model/BenefitPackage.java`
- [ ] T005 [P] 创建 TenantBenefitPackage JPA 实体 `backend/src/main/java/com/caa/school/model/TenantBenefitPackage.java`
- [ ] T006 [P] 创建 PromotionalSlot JPA 实体（含 SlotPosition 枚举） `backend/src/main/java/com/caa/school/model/PromotionalSlot.java`
- [ ] T007 [P] 创建 BenefitPackageRepository `backend/src/main/java/com/caa/school/repository/BenefitPackageRepository.java`
- [ ] T008 [P] 创建 TenantBenefitPackageRepository `backend/src/main/java/com/caa/school/repository/TenantBenefitPackageRepository.java`
- [ ] T009 [P] 创建 PromotionalSlotRepository（含按 tenantId 计数查询） `backend/src/main/java/com/caa/school/repository/PromotionalSlotRepository.java`
- [ ] T010 [P] 创建 GET /benefit-packages 和 GET /permissions 公共接口 `backend/src/main/java/com/caa/school/controller/BenefitPackageController.java`

---

## Phase 3：US1 — 系统管理员新建学校

**目标**: SYSTEM_ADMIN 可原子创建学校（含品牌/套餐/权限/运营位），默认管理员账户同步创建

**独立验收**: POST /schools 返回 201，学校+默认管理员在同一事务创建，School Code 重复返回 SCHOOL_CODE_DUPLICATE

- [ ] T011 [US1] 编写 SchoolService 测试：createSchool 原子事务（成功路径/Code重复/套餐不存在/运营位超10条） `backend/src/test/java/com/caa/school/service/SchoolServiceTest.java`
- [ ] T012 [US1] 实现 SchoolService.createSchool（@Transactional，创建 tenant+默认管理员+套餐关联+权限+运营位） `backend/src/main/java/com/caa/school/service/SchoolService.java`
- [ ] T013 [P] [US1] 编写 SchoolService.getLogoUploadUrl 测试（mock MinIO） `backend/src/test/java/com/caa/school/service/SchoolServiceTest.java`
- [ ] T014 [US1] 实现 SchoolService.getLogoUploadUrl（MinIO 预签名 URL） `backend/src/main/java/com/caa/school/service/SchoolService.java`
- [ ] T015 [US1] 创建 SchoolController：POST /schools、GET /schools/{id}、GET /schools/{id}/logo-upload-url，添加 @Operation/@ApiResponse `backend/src/main/java/com/caa/school/controller/SchoolController.java`

---

## Phase 4：US2 — 学校列表与编辑

**目标**: SYSTEM_ADMIN 可分页搜索学校列表，编辑学校所有信息，停用/启用学校

**独立验收**: GET /schools 返回分页结果，PUT /schools/{id} 更新成功，PATCH /schools/{id}/status 切换状态

- [ ] T016 [US2] 编写 SchoolService 测试：listSchools 分页+name/code 模糊搜索，updateSchool，updateStatus `backend/src/test/java/com/caa/school/service/SchoolServiceTest.java`
- [ ] T017 [US2] 实现 SchoolService.listSchools（JPA Specification 动态条件，AND 逻辑） `backend/src/main/java/com/caa/school/service/SchoolService.java`
- [ ] T018 [US2] 实现 SchoolService.updateSchool、updateStatus `backend/src/main/java/com/caa/school/service/SchoolService.java`
- [ ] T019 [US2] 扩展 SchoolController：GET /schools、PUT /schools/{id}、PATCH /schools/{id}/status `backend/src/main/java/com/caa/school/controller/SchoolController.java`

---

## Phase 5：US3 — 创建账户

**目标**: SCHOOL_ADMIN 可创建账户（含第二身份规则校验、email/phone 格式校验、密码策略）

**独立验收**: POST /schools/{id}/accounts 返回 201，登录名重复返回 LOGIN_NAME_DUPLICATE，第二身份不合法返回 SECONDARY_ROLE_INVALID

- [ ] T020 [US3] 编写 SchoolAccountService 测试：createAccount（成功/登录名重复/第二身份非法/email格式错/phone格式错/密码留空使用默认） `backend/src/test/java/com/caa/school/service/SchoolAccountServiceTest.java`
- [ ] T021 [US3] 实现 SchoolAccountService.createAccount（第二身份规则校验、email/phone 正则、tenantId 隔离） `backend/src/main/java/com/caa/school/service/SchoolAccountService.java`
- [ ] T022 [US3] 创建 SchoolAccountController：POST /schools/{schoolId}/accounts，@Operation/@ApiResponse `backend/src/main/java/com/caa/school/controller/SchoolAccountController.java`

---

## Phase 6：US4 — 账户列表查询

**目标**: SCHOOL_ADMIN 可按 loginName/accountType/name 多条件 AND 查询，分页展示

**独立验收**: GET /schools/{id}/accounts 支持三个可选查询参数，多条件同时传入取交集

- [ ] T023 [US4] 编写 SchoolAccountService 测试：listAccounts 单条件/多条件 AND/全空条件 `backend/src/test/java/com/caa/school/service/SchoolAccountServiceTest.java`
- [ ] T024 [US4] 实现 SchoolAccountService.listAccounts（JPA Specification AND 逻辑，tenantId 过滤） `backend/src/main/java/com/caa/school/service/SchoolAccountService.java`
- [ ] T025 [US4] 扩展 SchoolAccountController：GET /schools/{schoolId}/accounts `backend/src/main/java/com/caa/school/controller/SchoolAccountController.java`

---

## Phase 7：US5 — 编辑/密码/状态/删除账户

**目标**: SCHOOL_ADMIN 可编辑账户（不可改身份）、重置密码、停用/启用、删除（不可删自身）

**独立验收**: PUT 不接受 accountType 字段，DELETE 自身返回 SELF_DELETE_FORBIDDEN

- [ ] T026 [US5] 编写 SchoolAccountService 测试：updateAccount/resetPassword/updateStatus/deleteAccount（含自身保护） `backend/src/test/java/com/caa/school/service/SchoolAccountServiceTest.java`
- [ ] T027 [US5] 实现 SchoolAccountService.updateAccount（排除身份字段）、resetPassword、updateStatus、deleteAccount `backend/src/main/java/com/caa/school/service/SchoolAccountService.java`
- [ ] T028 [US5] 扩展 SchoolAccountController：PUT/PATCH password/PATCH status/DELETE 端点 `backend/src/main/java/com/caa/school/controller/SchoolAccountController.java`

---

## Phase 8：US6 — 批量导入账户

**目标**: SCHOOL_ADMIN 上传 Excel/CSV，部分成功策略，接口幂等，结果下载文件含密码

**独立验收**: 50条含2条重复登录名 → 48成功+2失败，报告文件可下载，重复提交返回相同结果

- [ ] T029 [US6] 编写 BatchImportService 测试：解析CSV/Excel、部分成功策略、幂等（重复登录名记入错误）、超1000条拒绝 `backend/src/test/java/com/caa/school/service/BatchImportServiceTest.java`
- [ ] T030 [US6] 实现 BatchImportService（Virtual Threads 解析，@Transactional 批量 saveAll，结果文件写 MinIO 返回下载 URL） `backend/src/main/java/com/caa/school/service/BatchImportService.java`
- [ ] T031 [US6] 扩展 SchoolAccountController：POST /schools/{schoolId}/accounts/batch-import `backend/src/main/java/com/caa/school/controller/SchoolAccountController.java`

---

## Phase 9：US7 — 批量修改/删除账户

**目标**: SCHOOL_ADMIN 批量修改账户状态，批量删除（自动排除自身，响应含实际删除数）

**独立验收**: 批量删除含自身 ID → excludedSelfCount=1，deletedCount 正确

- [ ] T032 [US7] 编写 SchoolAccountService 测试：batchUpdateStatus/batchDelete（含自身排除逻辑） `backend/src/test/java/com/caa/school/service/SchoolAccountServiceTest.java`
- [ ] T033 [US7] 实现 SchoolAccountService.batchUpdateStatus、batchDelete（排除 currentAccountId） `backend/src/main/java/com/caa/school/service/SchoolAccountService.java`
- [ ] T034 [US7] 扩展 SchoolAccountController：PATCH /batch-status、DELETE /batch `backend/src/main/java/com/caa/school/controller/SchoolAccountController.java`

---

## Phase 10：前端实现

> **前置**: 每个页面实现前必须调用 `/frontend-design` 技能，用户确认设计方案后方可开始

- [ ] T035 [P] 编写 schoolApi.ts 所有接口的 mock 测试（18 个端点） `frontend/src/lib/__tests__/schoolApi.test.ts`
- [ ] T036 [P] 实现 schoolApi.ts `frontend/src/lib/schoolApi.ts`
- [ ] T037 [P] [US1] 实现学校列表页（搜索/分页/状态列/新建入口） `frontend/src/app/admin/schools/page.tsx`
- [ ] T038 [P] [US1] 实现新建/编辑学校页（套餐自动勾选权限、Logo MinIO 直传、运营位增删排序） `frontend/src/app/admin/schools/[id]/page.tsx`
- [ ] T039 [P] [US3] 实现账户管理页（列表/查询/创建/编辑/删除） `frontend/src/app/admin/schools/[id]/accounts/page.tsx`
- [ ] T040 [US6] 实现批量导入对话框组件（文件上传/结果下载） `frontend/src/components/school/BatchImportDialog.tsx`

---

## Phase 11：质量门禁

- [ ] T041 后端编译通过：`mvn compile` 0 error, 0 warning
- [ ] T042 后端测试通过：`mvn test` 核心模块覆盖率 ≥ 80%，0 failure
- [ ] T043 前端编译通过：`pnpm build` 0 error
- [ ] T044 前端测试通过：`npm test -- --run` 全部通过

---

## 依赖关系

```
Phase 1（T001 DB）
  └─→ Phase 2（T002–T010 基础设施，全部可并行）
        ├─→ Phase 3（T011–T015 US1 新建学校）
        └─→ Phase 4（T016–T019 US2 列表/编辑）[可与 Phase 3 并行]
              ├─→ Phase 5（T020–T022 US3 创建账户）
              ├─→ Phase 6（T023–T025 US4 账户列表）[可与 Phase 5 并行]
              └─→ Phase 7（T026–T028 US5 编辑/删除）[可与 Phase 5,6 并行]
                    ├─→ Phase 8（T029–T031 US6 批量导入）
                    └─→ Phase 9（T032–T034 US7 批量操作）[可与 Phase 8 并行]
                          └─→ Phase 10（T035–T040 前端）
                                └─→ Phase 11（T041–T044 质量门禁）
```

**并行机会**:
- T002–T010 全部可并行（不同文件）
- Phase 3 + Phase 4 可并行（不同 Service 方法）
- Phase 5 + Phase 6 + Phase 7 可并行
- Phase 8 + Phase 9 可并行
- T037 + T038 + T039 可并行

---

## MVP 范围建议

**MVP（P0，可独立交付）**: Phase 1–7（T001–T028）
后端 P0 全部 REST API 可用，质量门禁通过，前端可用 Swagger UI 验证。

**完整交付**: Phase 1–11（T001–T044）
