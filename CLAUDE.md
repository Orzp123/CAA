# CLAUDE.md — CAA Project Guide

## What is this project?

CAA (Claude Agent Assembly) — 可视化 AI Agent 工作流构建平台。用户通过拖拽界面设计、部署、监控 AI Agent 工作流。对标 Dify / Coze / n8n。

## Tech Stack

- **Backend**: Java 21 · Spring Boot 4.1.0 · Spring AI 2.0.0 · Temporal · MySQL 8.4 · Redis 7 · Kafka (KRaft) · Doris · Flink · MinIO · Nacos
- **Frontend**: Next.js 15 · React 19 · React Flow · Amis Editor · Tailwind 4 · Zustand
- **Infra**: Docker Compose (13 services) · Nginx

## Build & Run

```bash
# Full stack
docker compose up -d

# Dev mode (infra only + local services)
docker compose up -d mysql redis kafka minio temporal temporal-ui
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && pnpm install && pnpm dev
```

## Project Structure

```
backend/src/main/java/com/caa/
├── agent/        Agent 管理（CRUD + 配置）
├── workflow/     工作流定义与执行（Temporal）
├── chat/         对话服务（Spring AI + SSE）
├── page/         页面设计器（Amis Schema）
├── event/        事件驱动（Kafka 生产/消费）
├── storage/      文件存储（MinIO）
├── config/       全局配置（Security, Kafka, Doris, MinIO, SpringAI）
└── common/       公共组件（ApiResponse, ErrorCode, TenantFilter）

frontend/src/
├── app/          Next.js App Router 页面
├── components/   功能组件（workflow/, chat/, designer/）
└── lib/          工具库（api.ts）
```

## Coding Conventions

### Backend (Java)
- Record 类作 DTO，禁止 Lombok
- sealed interface + pattern matching
- Virtual Threads 处理阻塞 I/O
- Spring AI Advisor 链，禁止裸调 ChatClient
- 统一返回 `ApiResponse<T>`，异常走 `@ControllerAdvice`
- 所有 Controller 需 `@Operation` + `@ApiResponse` 注解
- 数据库字段 snake_case，Java 属性 camelCase

### Frontend (TypeScript)
- React 19 use() hook · Server Components · Server Actions
- App Router only，禁止 Pages Router
- Zustand 状态管理，禁止 Redux
- Tailwind CSS 4，禁止内联 style
- TypeScript strict，禁止 any

## Key Constraints

- **Multi-tenant**: 所有数据模型包含 tenantId，查询自动注入租户过滤
- **Spec-first**: 功能实现前必须有 spec 文档
- **Test-first**: Spec → 测试 → 实现 → 通过
- **Blacklist**: Lombok / MyBatis / ZooKeeper / MongoDB / jQuery / Angular / Vue

## Quality Gates

- 编译 0 error, 0 warning
- 单测覆盖 ≥ 80%
- 0 High/Critical CVE
- API 文档注解完整
- Docker 全服务启动无报错
