-- Apache Doris analytics database init
-- Run against Doris FE after cluster is healthy

CREATE DATABASE IF NOT EXISTS caa_analytics;

USE caa_analytics;

-- Agent usage statistics (aggregate per day)
CREATE TABLE IF NOT EXISTS agent_usage_stats (
    stat_date       DATE         NOT NULL,
    agent_id        VARCHAR(36)  NOT NULL,
    agent_name      VARCHAR(100) NOT NULL,
    provider        VARCHAR(50)  NOT NULL,
    model           VARCHAR(100) NOT NULL,
    request_count   BIGINT       NOT NULL DEFAULT 0,
    success_count   BIGINT       NOT NULL DEFAULT 0,
    error_count     BIGINT       NOT NULL DEFAULT 0,
    total_tokens    BIGINT       NOT NULL DEFAULT 0,
    avg_latency_ms  DOUBLE       NOT NULL DEFAULT 0.0
)
DUPLICATE KEY(stat_date, agent_id)
DISTRIBUTED BY HASH(agent_id) BUCKETS 4
PROPERTIES (
    "replication_num" = "1",
    "storage_medium" = "HDD"
);

-- Workflow execution metrics
CREATE TABLE IF NOT EXISTS workflow_execution_metrics (
    execution_id        VARCHAR(64)  NOT NULL,
    workflow_id         VARCHAR(36)  NOT NULL,
    workflow_name       VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    started_at          DATETIME     NOT NULL,
    completed_at        DATETIME,
    duration_ms         BIGINT,
    step_count          INT          NOT NULL DEFAULT 0,
    error_message       TEXT
)
DUPLICATE KEY(execution_id, workflow_id)
DISTRIBUTED BY HASH(workflow_id) BUCKETS 4
PROPERTIES (
    "replication_num" = "1",
    "storage_medium" = "HDD"
);

-- Chat session analytics
CREATE TABLE IF NOT EXISTS chat_session_analytics (
    session_date        DATE         NOT NULL,
    session_id          VARCHAR(64)  NOT NULL,
    agent_id            VARCHAR(36)  NOT NULL,
    message_count       INT          NOT NULL DEFAULT 0,
    user_message_count  INT          NOT NULL DEFAULT 0,
    ai_message_count    INT          NOT NULL DEFAULT 0,
    total_tokens        BIGINT       NOT NULL DEFAULT 0,
    session_duration_s  BIGINT
)
DUPLICATE KEY(session_date, session_id)
DISTRIBUTED BY HASH(session_id) BUCKETS 4
PROPERTIES (
    "replication_num" = "1",
    "storage_medium" = "HDD"
);
