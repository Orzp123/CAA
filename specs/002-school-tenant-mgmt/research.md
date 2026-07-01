# Research: feature/002 学校租户管理与账户管理

**日期**: 2026-06-25
**基于**: spec.md + V1__auth_schema.sql + 现有 auth 包

---

## 决策 1：品牌字段存放位置

- **决策**: 在 `tenants` 表增加列（V2 migration ALTER TABLE），不建独立表
- **理由**: 品牌字段与租户 1:1，无查询独立性需求，合并查询减少 JOIN
- **备选**: `tenant_brand_configs` 独立表 — 放弃，过度设计

## 决策 2：第二身份实现方式

- **决策**: 在 `accounts` 表增加 `secondary_role` ENUM('TEACHER','ASSISTANT') NULL 列
- **理由**: 第二身份与账户 1:1，无多值需求；ENUM 便于索引和校验
- **备选**: 独立 `account_roles` 多行表 — 放弃，规格明确最多一个第二身份

## 决策 3：权益套餐（BenefitPackage）

- **决策**: 新建 `benefit_packages` 表 + `tenant_benefit_packages` 关联表；本期预置数据，不提供 CRUD API
- **理由**: 套餐是独立业务实体，与租户 N:1（一个租户选一个套餐）
- **约束**: 套餐中的 `default_permission_codes` 用 JSON 列存储，便于自动勾选

## 决策 4：运营位（PromotionalSlot）

- **决策**: 新建 `promotional_slots` 表，position 用 ENUM('HOME_TOP_BANNER','HOME_SIDEBAR','LOGIN_BANNER')
- **理由**: 独立实体，支持多条（上限 10）、有序排列
- **排序**: `sort_order INT` 列，前端可调整顺序

## 决策 5：Logo 上传

- **决策**: 前端直接上传到 MinIO 预签名 URL，后端仅存储 `logo_url` 路径
- **理由**: 宪法明确"文件上传走 MinIO 预签名 URL 直传，禁止后端中转大文件"
- **流程**: 前端调 `GET /api/v1/schools/{id}/logo-upload-url` → 获取预签名 URL → 直传 MinIO → PUT logo_url 回写

## 决策 6：批量导入处理方式

- **决策**: 同步处理（≤1000 条，30 秒内），使用 Virtual Threads 解析 CSV；结果文件写 MinIO 返回下载 URL
- **理由**: 1000 条账户创建估算 <10 秒，无需 Kafka 异步队列；宪法允许 Virtual Threads 处理阻塞 I/O
- **幂等**: 登录名已存在记入错误文件，错误原因"登录名已存在"，不中断其余行

## 决策 7：新包还是扩展 auth 包

- **决策**: 新建 `com.caa.school` 包，包含 controller/service/repository/model/dto
- **理由**: 宪法"后端按领域分包，包间通过接口通信"；学校管理与认证是不同领域
- **复用**: 通过 Spring Bean 注入复用 `AccountRepository`、`TenantRepository`（auth 包公开接口）

## 决策 8：email/phone 字段

- **决策**: V2 migration 在 `accounts` 表增加 `email VARCHAR(256) NULL` + `phone VARCHAR(32) NULL`
- **校验**: 服务层正则校验（email RFC 5322 简化版；phone 中国手机号 1[3-9]\d{9}）

---

## 宪法符合性检查

| 原则 | 状态 |
|------|------|
| Spec-First | ✅ spec.md 完整，16/16 通过 |
| API-First | ✅ 所有功能先以 REST API 暴露 |
| Multi-Tenant by Design | ✅ 所有新表含 tenant_id |
| Test-First | ✅ 计划阶段 TDD 流程约束 |
| 禁止项检查 | ✅ 无 Lombok/MyBatis/MongoDB |
| 性能设计 | ✅ 列表分页、MinIO 直传、Virtual Threads |
| Fail Loud | ✅ 批量导入错误写入文件，不吞异常 |
