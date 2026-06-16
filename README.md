# CAA — Claude Agent Assembly

Visual AI agent workflow builder. Design, deploy, and monitor AI agents with a drag-and-drop interface.

**Stack:** Spring Boot 4.1.0 · Spring AI 2.0.0 · Next.js 15 · React Flow · Amis/Amis-Editor · Temporal · MySQL 8.4 · Redis · Kafka · Apache Doris · Apache Flink · MinIO · Docker

---

## Quick Start

```bash
# 1. Copy env template and fill in your API keys
cp .env.example .env

# 2. Start all services
docker compose up -d

# 3. Open in browser
# Frontend:     http://localhost
# Backend API:  http://localhost/api/actuator/health
# Temporal UI:  http://localhost:8088
# Flink UI:     http://localhost:8081
# MinIO Console:http://localhost:9001
# Doris FE:     http://localhost:8030
```

## Development

```bash
# Start infra only
docker compose up -d mysql redis kafka minio temporal temporal-ui

# Backend (hot reload via spring-boot:run)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (turbopack dev server)
cd frontend && pnpm install && pnpm dev
```

## Structure

```
CAA/
├── backend/          Spring Boot 4.1.0 + Spring AI 2.0.0
├── frontend/         Next.js 15 + React Flow + Amis Editor
├── infra/
│   ├── mysql/        MySQL init SQL (schema + seed data)
│   ├── doris/        Apache Doris analytics tables
│   ├── minio/        MinIO bucket init script
│   └── nginx/        Reverse proxy config
├── docker-compose.yml
├── docker-compose.dev.yml
└── .env.example
```

## Services

| Service          | Port(s)        | Description                        |
|-----------------|----------------|------------------------------------|
| nginx            | 80             | Reverse proxy                      |
| frontend         | 3000           | Next.js app                        |
| backend          | 8080           | Spring Boot API                    |
| mysql            | 3306           | MySQL 8.4 (primary database)       |
| redis            | 6379           | Cache + vector store               |
| kafka            | 9092           | Message queue (KRaft mode)         |
| doris-fe         | 8030 / 9030    | Doris Frontend (HTTP / MySQL proto)|
| doris-be         | 8040           | Doris Backend                      |
| flink-jobmanager | 8081           | Flink Web UI + job submission      |
| minio            | 9000 / 9001    | S3-compatible storage / console    |
| temporal         | 7233           | Workflow engine                    |
| temporal-ui      | 8088           | Temporal web UI                    |
