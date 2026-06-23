# CLAUDE.md — CAA Project Guide

## 行为规范（强制，无例外）

### 模型路由

sonnet 路由器，实际工作按任务类型派子代理执行。禁止路由器直接写代码。

| 模型 | 适用 |
|------|------|
| haiku | 搜索、简单查询、记忆、问答 |
| sonnet | 代码实现、审查、测试、修复、重构 |
| opus | 深度分析、跨模块架构、疑难根因 |

### 子代理 / 子 Agent 规范（强制，无例外）

**所有子代理、子 Agent 必须与主会话遵守完全相同的行为准则。主会话派发任务时必须在 prompt 中明确传达以下规则：**

1. **GateGuard**：每次 Write/Edit 前在文本中紧接声明 4 个事实：调用方文件（Grep 结果）、无重复文件（Glob 结果）、数据字段结构、用户指令原文
2. **执行纪律**：不确定就停止报告，不猜；没要求的不写；只改被要求的部分；任务未完成禁止停止
3. **质量门禁**：完成后必须运行编译/测试，贴出结果；不得声明成功而不验证
4. **禁止项**：Lombok · MyBatis · ZooKeeper · MongoDB · jQuery/Angular/Vue；TypeScript 禁 any；禁内联 style
5. **TDD**：先写测试，再写实现，再重构
6. **成本意识**：遇到 COST WARNING / COST CRITICAL 立即停止并向主会话报告，不自行决定继续
7. **语言**：所有输出、报告、注释使用中文（代码标识符除外）
8. **不再嵌套**：子代理本身可写代码，但不得向下再派子代理执行代码

### 执行纪律

- 不确定就问，别猜
- 没要求的不写
- 只改被要求的部分
- 给验收标准，别给步骤
- 任务未完成禁止停止，失败主动咨询用户
- 提议最多一次，不追问
- Context7 优先查文档，禁依赖训练数据
- Graph (CRG) 优先于 Grep/Glob/Read → 详见 `.claude/docs/crg-workflow.md`
- Shell 命令强制 `rtk` 前缀 → 详见 `.claude/docs/rtk-commands.md`
- 工具选择查路由表 → 详见 `.claude/docs/tool-routing.md`

### 质量门禁

1. TDD：RED → GREEN → REFACTOR
2. 完成前贴编译/测试结果
3. 连续 3 次失败 → 停，请求人工
4. Bug 修复不顺手重构

## 项目概述

CAA (Claude Agent Assembly) — 可视化 AI Agent 工作流构建平台（对标 Dify/Coze/n8n）。三种 Agent 类型：对话流、工作流、自定义页面工作流。

## Tech Stack

- **Backend**: Java 21 · Spring Boot 4.1.0 · Spring AI 2.0.0 · Temporal · MySQL 8.4 · Redis 7 · Kafka (KRaft) · Doris · Flink · MinIO · Nacos
- **Frontend**: Next.js 15 · React 19 · React Flow · Amis Editor · Tailwind 4 · Zustand
- **Infra**: Docker Compose (14 services) · Nginx

基础设施详情 → `.claude/docs/infra-guide.md`

## Project Structure

```
backend/src/main/java/com/caa/
├── agent/      Agent 管理        ├── workflow/  Temporal 工作流
├── chat/       对话 (SSE)        ├── page/      Amis Schema
├── event/      Kafka 事件        ├── storage/   MinIO 文件
├── config/     全局配置          └── common/    ApiResponse, TenantFilter

frontend/src/
├── app/        App Router        ├── components/ workflow/chat/designer
└── lib/        api.ts
```

## Coding Conventions

### Backend (Java)
- Record DTO，禁止 Lombok
- sealed interface + pattern matching
- Virtual Threads 处理阻塞 I/O
- Spring AI Advisor 链，禁止裸调 ChatClient
- 统一 `CommonResult<T>`，异常 `@ControllerAdvice`
- Controller 必须 `@Operation` + `@ApiResponse`
- DB snake_case，Java camelCase
- 完整规范 → `.claude/docs/backend-conventions.md`（15 章）

### Frontend (TypeScript)
- React 19 use() · Server Components · Server Actions
- App Router only，禁 Pages Router
- Zustand，禁 Redux
- Tailwind CSS 4，禁内联 style
- TypeScript strict，禁 any
- UI 设计必须调用 `/frontend-design` 技能，禁止模板化默认外观
- 完整规范 → `.claude/docs/frontend-conventions.md`（15 章）

## Key Constraints

- **Multi-tenant**: 所有表含 tenantId，查询自动注入租户过滤
- **Blacklist**: Lombok / MyBatis / ZooKeeper / MongoDB / jQuery / Angular / Vue

## 开发工作流（四层架构，宪法强制）

```
需求(spec-kit) → 实现(superpowers) → 审查(ECC) → 交付(superpowers)
底层加速: code-review-graph | 持续学习: ECC
```

- **新功能**: `/speckit:specify` → `clarify` → `plan` → `tasks` → superpowers 执行 → ECC 审查
- **Bug 修复**: superpowers systematic-debugging + `query_graph` 定位
- **重构**: `refactor_tool` → `/ecc:refactor-clean`
- **TDD**: superpowers 主导（先写代码就删除），ECC 补充审查

宪法路径: `.specify/memory/constitution.md`（v1.1.0，最高优先级）

完整流程详见 → `.claude/docs/dev-workflow.md`

## Quality Gates

- 编译 0 error, 0 warning
- 单测覆盖 ≥ 80%
- 0 High/Critical CVE
- API 文档注解完整
- Docker 全服务启动无报错

## 索引文档

| 文件 | 内容 |
|------|------|
| `.claude/docs/dev-workflow.md` | 四层架构完整开发工作流（阶段分工、场景速查、冲突规避） |
| `.claude/docs/backend-conventions.md` | 后端开发规范 15 章（异常码/分层/事务/日志/REST/校验/安全/多租户/缓存/DB/异步/配置/API文档/枚举/测试） |
| `.claude/docs/frontend-conventions.md` | 前端开发规范 15 章（结构/组件/Zustand/数据获取/TS/样式/表单/路由/错误/性能/测试/安全/质量/i18n/a11y） |
| `.claude/docs/rtk-commands.md` | RTK 完整命令参考 |
| `.claude/docs/tool-routing.md` | 工具/技能选择路由表 |
| `.claude/docs/crg-workflow.md` | code-review-graph 工作流 |
| `.claude/docs/infra-guide.md` | 基础设施、启动命令、管理入口、设计决策 |

<!-- SPECKIT START -->
## 当前活跃功能计划

| 文件 | 内容 |
|------|------|
| `specs/001-multi-auth-tenant-rbac/plan.md` | 多租户用户认证与权限管理系统实现计划 |
| `specs/001-multi-auth-tenant-rbac/spec.md` | 功能规格（50/50 检查项通过） |
| `specs/001-multi-auth-tenant-rbac/data-model.md` | 数据模型（9 张表 + Redis 缓存设计） |
| `specs/001-multi-auth-tenant-rbac/contracts/api-contracts.md` | API 契约（认证/租户/权限接口） |
| `specs/001-multi-auth-tenant-rbac/research.md` | 技术决策（网关/JWT/微信/SSO/黑名单） |
<!-- SPECKIT END -->
