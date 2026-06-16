package com.caa.common;

/**
 * Application-level error codes.
 * Each constant carries a short code string and a default human-readable message.
 */
public enum ErrorCode {

    // Generic
    INTERNAL_ERROR("ERR_INTERNAL", "An unexpected error occurred"),
    VALIDATION_ERROR("ERR_VALIDATION", "Request validation failed"),
    NOT_FOUND("ERR_NOT_FOUND", "Resource not found"),
    CONFLICT("ERR_CONFLICT", "Resource already exists"),
    UNAUTHORIZED("ERR_UNAUTHORIZED", "Authentication required"),
    FORBIDDEN("ERR_FORBIDDEN", "Insufficient permissions"),

    // Tenant
    TENANT_MISSING("ERR_TENANT_MISSING", "X-Tenant-Id header is required"),

    // Agent
    AGENT_NOT_FOUND("ERR_AGENT_NOT_FOUND", "Agent not found"),
    AGENT_NAME_CONFLICT("ERR_AGENT_CONFLICT", "Agent name already exists for this tenant"),

    // Workflow
    WORKFLOW_NOT_FOUND("ERR_WORKFLOW_NOT_FOUND", "Workflow not found"),

    // Page
    PAGE_NOT_FOUND("ERR_PAGE_NOT_FOUND", "Page not found"),
    PAGE_PATH_CONFLICT("ERR_PAGE_PATH_CONFLICT", "Page path already exists for this tenant"),

    // Chat
    CHAT_AGENT_UNAVAILABLE("ERR_CHAT_AGENT", "Chat agent is unavailable");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
