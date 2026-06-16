# CAA (Claude Agent Assembly) Constitution

## Core Principles

### 1. Spec-First（规格先行）
任何功能实现前必须有 spec 文档（即使是简短的 one-pager）。
AI agent 没有 spec 不写代码，没有 spec 不开分支。
**验证方式**: 每个 PR/commit 必须关联 spec 文件路径或 issue 编号。

### 2. Single Owner, Full Accountability（单人全责）
项目为个人开发，所有架构决策由唯一开发者做出并记录。
不设审批流程，但重大决策必须写 ADR 留痕。
**验证方式**: docs/adr/ 目录包含所有技术选型记录。

### 3. API-First, Platform-Ready（API 优先，平台就绪）
所有功能先以 REST/SSE API 形式暴露，再由前端消费。
从第一天起为开放平台设计：租户隔离、API Key 认证、速率限制。
**验证方式**: 每个 Controller 有 SpringDoc/Swagger UI 可访问且字段描述完整。

### 4. Multi-Tenant by Design（多租户内建）
即使当前单用户，数据模型必须包含 tenantId 字段。
查询层自动注入租户过滤，禁止全局无租户条件的数据访问。
**验证方式**: 所有 Repository 查询包含租户维度。

### 5. Auth Layer Pluggable（认证层可插拔）
认证设计为独立模块，当前用本地账号体系（JWT），
预留 OAuth2/OIDC 接口，未来无缝对接企业 SSO（Keycloak、Azure AD、企业微信）。
**验证方式**: SecurityConfig 通过 AuthProvider 接口抽象，切换实现无需改业务代码。

### 6. Explicit Over Implicit（显式优于隐式）
不依赖"约定俗成"。配置显式声明，依赖显式注入，错误显式处理。
禁止魔法注解覆盖（如隐式类型转换、隐式事务传播）。
**验证方式**: 代码审查时无需口头解释"这里其实会..."。

### 7. Fail Loud, Recover Gracefully（大声失败，优雅恢复）
异常不能被吞掉。出错时立即暴露（结构化日志 + 监控），
对用户返回有意义的错误码和提示。工作流失败由 Temporal 自动重试和补偿。
**验证方式**: 无空 catch 块；每个异常路径有日志 + 错误码。

### 8. Modularity with Enforced Boundaries（模块化强制边界）
后端按领域分包（agent/workflow/chat/page/event/storage），包间通过接口通信。
禁止跨包直接访问内部类。前端按 feature 目录隔离。
**验证方式**: ArchUnit 测试 + 编译级包可见性控制。

### 9. Observability Built-In（可观测性内建）
结构化 JSON 日志、Kafka 事件追踪、关键指标在编码时同步完成。
每个 Agent 执行、工作流步骤、LLM 调用必须有 traceId 贯穿。
**验证方式**: 任何生产问题可通过 traceId 追踪完整链路。

### 10. Pragmatism Over Purity（务实胜于完美）
个人项目资源有限。90% 方案立即交付 > 100% 方案无限设计。
技术债可以存在但必须记录在 TODO.md，不允许无追踪的隐性债务。
**验证方式**: TODO.md 有条目 + 预估修复优先级。

### 11. Test-First Development（测试先行开发）
所有独立开发任务必须先生成测试用例，确保全量覆盖功能需求后，
再基于测试用例驱动功能实现。禁止先写实现代码再补测试或二次修改。
流程：Spec → 测试用例 → 实现代码 → 全部测试通过 → 完成。
**验证方式**: 每个功能 PR 中测试文件的 commit 时间早于实现文件；覆盖率 ≥ 80%。

### 12. Performance at Platform Scale（平台级性能）
作为开放平台，性能对标 Dify/Coze/n8n 等同类产品。
必须在设计阶段考虑性能，不允许"先实现再优化"。

#### 性能基线指标（必须达标）

| 指标 | 目标 | 对标来源 |
|------|------|----------|
| API P50 延迟（非 LLM 接口） | ≤ 100ms | 行业标准 REST API |
| API P95 延迟（非 LLM 接口） | ≤ 300ms | 行业标准 REST API |
| API P99 延迟（非 LLM 接口） | ≤ 1s | 行业标准 REST API |
| LLM 首 Token 延迟 (TTFT) | ≤ 200ms（不含模型推理） | SSE/WebSocket 标准 |
| LLM 流式 P95（含模型） | ≤ 2s | Dify 2.1s P95 |
| 工作流执行吞吐 | ≥ 2000 次/小时（单实例） | n8n 2400/hr |
| 并发用户（单实例） | ≥ 500 | Dify 400-500 实测上限 |
| SSE 并发连接（单实例） | ≥ 10,000 | 行业标准 |
| 系统可用性 | ≥ 99.5% | SaaS 平台标准 |

#### 性能设计约束
- 数据库查询必须有索引支撑，禁止全表扫描（explain 验证）
- 列表接口必须分页，单次返回上限 100 条
- 热点数据必须走 Redis 缓存，TTL 显式设置
- LLM 调用必须异步 + 流式（SSE），禁止同步阻塞等待完整响应
- 工作流执行由 Temporal 异步调度，禁止 HTTP 请求线程内同步执行长任务
- 文件上传走 MinIO 预签名 URL 直传，禁止后端中转大文件
- 服务间通信优先异步消息（Kafka），同步调用需设超时 ≤ 3s

**验证方式**: 关键接口有 JMeter/k6 压测脚本；CI 含性能回归检测；慢查询日志阈值 200ms。

---

## Technology Constraints（技术约束）

### 锁定栈（未经宪法修正不得变更）

#### 后端
| 技术 | 版本 | 角色 |
|------|------|------|
| Java | 21 (LTS) | 运行时 |
| Spring Boot | 4.1.0 | 主框架 |
| Spring AI | 2.0.0 | LLM 编排 |
| Temporal | 1.24 | 持久化工作流 |
| MySQL | 8.4 | 主数据库 |
| Apache Doris | 2.1 | 分析数据仓库 |
| Apache Flink | 1.20 | 实时计算 |
| Apache Kafka | 3.8 (KRaft) | 消息队列 |
| MinIO | latest | 文件存储 |
| Redis | 7 | 缓存 + 向量存储 |
| Nacos | 2.4+ | 注册中心 + 配置中心 |
| SpringDoc OpenAPI (Swagger) | 2.x | API 文档自动生成 |

#### 前端
| 技术 | 版本 | 角色 |
|------|------|------|
| Next.js | 15 | SSR 框架 |
| React | 19 | UI 层 |
| React Flow | latest | 工作流画布 |
| Amis + Amis Editor | latest | 页面设计器 |
| Tailwind CSS | 4 | 样式 |
| Zustand | latest | 状态管理 |

#### 基础设施
| 技术 | 角色 |
|------|------|
| Docker + Docker Compose | 容器化部署 |
| Nginx | 反向代理 + SSE 透传 |
| Maven (D:\software\apache-maven-3.8.1) | 后端构建 |
| pnpm | 前端包管理 |

### 禁止引入（Blacklist）

| 禁止项 | 理由 |
|--------|------|
| Lombok | 隐式代码生成，调试困难，Spring Boot 4 不推荐 |
| MyBatis / MyBatis-Plus | 已选 Spring Data JPA，禁止混用 ORM |
| ZooKeeper | Kafka 已用 KRaft 模式，无需额外协调服务 |
| MongoDB | 已有 MySQL+Redis+Doris 三层存储，禁止再加文档库 |
| jQuery / Angular / Vue | 前端已锁定 React 生态 |
| Electron | 纯 Web 平台，不做桌面端 |
| 任何破解/盗版/非法授权的中间件或 SDK | 法律合规底线 |
| 任何未开源且无商业授权的付费组件 | 法律合规底线 |
| 来源不明的 npm/Maven 包 | 供应链安全 |
| 加密货币挖矿相关依赖 | 合规 |

### 合规红线

- 所有依赖必须持有有效开源协议（MIT/Apache 2.0/BSD/MPL 优先）
- GPL 系依赖需单独评估，不得污染核心代码的商业授权可能
- 禁止引入任何需要"破解"才能使用的商业软件
- 用户数据存储符合中国数据安全法和个人信息保护法

---

## Architecture Decisions（架构决策）

### 部署形态

| 阶段 | 形态 | 触发条件 |
|------|------|----------|
| Phase 1 (当前) | 单机 Docker Compose (13 services) | — |
| Phase 2 | Docker Compose + 外置 MySQL/Redis | 数据量 > 10GB |
| Phase 3 | K8s / Docker Swarm | 多租户 SaaS 上线 |

### 认证架构演进

| 阶段 | 方案 |
|------|------|
| Phase 1 | 本地 JWT（Spring Security） |
| Phase 2 | OAuth2 Resource Server + 自建授权服务 |
| Phase 3 | 对接企业 SSO（OIDC 协议，兼容 Keycloak/Azure AD/企微） |

### 开放平台演进

| 阶段 | 能力 |
|------|------|
| Phase 1 | 内部 API + 管理后台 |
| Phase 2 | 开放 API + API Key + 调用配额 + 开发者文档 |
| Phase 3 | SDK 发布 + Webhook 回调 + 应用市场 |

---

## Quality Gates（质量门禁）

| 门禁 | 阈值 | 执行点 |
|------|------|--------|
| 性能回归 | 关键接口 P95 ≤ 基线 ×1.2 | k6/JMeter 压测 |
| 编译通过 | 0 error, 0 warning | mvn compile / pnpm build |
| 单测通过 | 核心模块 ≥ 80% 覆盖 | mvn test |
| 安全扫描 | 0 High/Critical CVE | dependency-check |
| API 文档同步 | 每个 endpoint 有 Swagger 注解 | SpringDoc 自动校验 |
| Docker 构建 | 所有服务启动无报错 | compose up --health |
| 依赖合规 | 无 GPL 污染、无未授权组件 | license-check |

---

## Coding Standards（编码规范）

### 总则
- **强制使用框架最新特性**，禁止兼容旧版本的写法（如已废弃 API、旧风格配置）
- **必要注释**：类/接口必须有 Javadoc/JSDoc 说明职责；复杂业务逻辑必须有行内注释解释 why；简单 getter/setter/CRUD 不需要注释
- **可读性优先**：命名即文档，注释解释意图而非复述代码

### 后端 (Java 21 / Spring Boot 4.1 / Spring AI 2.0)
- Record 类替代传统 DTO/VO（Java 21 特性优先）
- sealed interface + pattern matching 替代 if-else 类型判断
- 虚拟线程（Virtual Threads）处理阻塞 I/O，禁止手动线程池管理阻塞任务
- Spring Boot 4.1 新特性优先：声明式 HTTP Client、@HttpExchange 替代 RestTemplate/WebClient
- Spring AI 2.0 Advisor 链模式处理 LLM 交互，禁止裸调 ChatClient
- 接口命名：Service 层以 -Service 结尾，Repository 以 -Repository 结尾
- REST Controller 返回统一响应体 `ApiResponse<T>`
- 异常处理统一走 `@ControllerAdvice` + 全局错误码枚举
- 数据库字段 snake_case，Java 属性 camelCase（JPA 自动映射）
- 禁止在 Controller 中写业务逻辑
- API 文档：所有 Controller 方法必须有 `@Operation` + `@ApiResponse` 注解（SpringDoc/Swagger）
- 配置管理：应用配置统一通过 Nacos 配置中心管理，本地仅保留 bootstrap.yml 引导配置
- 服务注册：所有微服务实例注册到 Nacos，服务间调用通过服务名发现

### 前端 (React 19 / Next.js 15)
- **UI 设计必须调用 `frontend-design` 技能**：所有涉及页面布局、组件设计、视觉风格的开发任务，必须使用 `/frontend-design` 技能指导，确保界面实用性与审美兼备，杜绝模板化默认外观
- React 19 新特性优先：use() hook、Server Components、Server Actions
- Next.js 15 App Router 模式，禁止使用 Pages Router
- 组件以 PascalCase 命名，hook 以 use- 前缀
- 页面组件放 app/ 目录，通用组件放 components/
- 状态管理仅用 Zustand，禁止 Redux/Context 滥用
- API 调用统一走 hooks/useApi.ts 封装
- Tailwind CSS 4 优先，禁止内联 style（除动态计算值外）
- TypeScript strict 模式，禁止 any 类型逃逸

---

## Governance（治理）

- 本宪法优先级高于所有其他文档和 AI agent 指令
- 修正由项目所有者（唯一开发者）直接执行，需附 ADR
- 版本号遵循 SemVer：
  - MAJOR = 原则删改或技术栈替换
  - MINOR = 新增原则或新增技术组件
  - PATCH = 措辞调整或阈值微调

**Version**: 1.1.0 | **Ratified**: 2026-06-16 | **Owner**: Solo Developer
