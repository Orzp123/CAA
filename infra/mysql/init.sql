-- MySQL init script for CAA
-- Character set: utf8mb4, Engine: InnoDB

CREATE DATABASE IF NOT EXISTS caa
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Nacos requires its own database
CREATE DATABASE IF NOT EXISTS nacos
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE caa;

-- Agents table
CREATE TABLE IF NOT EXISTS agents (
    id           CHAR(36)      NOT NULL DEFAULT (UUID()),
    tenant_id    VARCHAR(64)   NOT NULL DEFAULT 'default',
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(500),
    provider     VARCHAR(50)   NOT NULL,
    model        VARCHAR(100)  NOT NULL,
    system_prompt TEXT,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    config       LONGTEXT,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_agents_tenant_name (tenant_id, name),
    KEY idx_agents_status (status),
    KEY idx_agents_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workflow definitions table
CREATE TABLE IF NOT EXISTS workflow_definitions (
    id                     CHAR(36)     NOT NULL DEFAULT (UUID()),
    tenant_id              VARCHAR(64)  NOT NULL DEFAULT 'default',
    name                   VARCHAR(100) NOT NULL,
    description            VARCHAR(500),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    graph_json             LONGTEXT,
    temporal_workflow_type VARCHAR(200),
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_workflow_status (status),
    KEY idx_workflow_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Page schemas table (Amis JSON pages)
CREATE TABLE IF NOT EXISTS page_schemas (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    tenant_id   VARCHAR(64)  NOT NULL DEFAULT 'default',
    name        VARCHAR(100) NOT NULL,
    path        VARCHAR(200),
    description VARCHAR(500),
    schema_json LONGTEXT     NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_page_schemas_tenant_path (tenant_id, path),
    KEY idx_page_schemas_status (status),
    KEY idx_page_schemas_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed a default agent
INSERT INTO agents (id, tenant_id, name, description, provider, model, system_prompt, status)
VALUES (
    UUID(),
    'default',
    'default-assistant',
    'Default AI assistant powered by GPT-4o',
    'openai',
    'gpt-4o',
    'You are a helpful AI assistant. Be concise and accurate.',
    'ACTIVE'
) ON DUPLICATE KEY UPDATE name = name;
