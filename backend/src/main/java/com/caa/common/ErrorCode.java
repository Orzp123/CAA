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
    CHAT_AGENT_UNAVAILABLE("ERR_CHAT_AGENT", "Chat agent is unavailable"),

    // Auth
    CAPTCHA_INVALID("ERR_CAPTCHA_INVALID", "Invalid or expired captcha code"),
    INVALID_CREDENTIALS("ERR_INVALID_CREDENTIALS", "Invalid credentials"),
    ACCOUNT_LOCKED("ERR_ACCOUNT_LOCKED", "Account is temporarily locked"),
    ACCOUNT_DISABLED("ERR_ACCOUNT_DISABLED", "Account is disabled"),

    // School management
    SCHOOL_CODE_DUPLICATE("ERR_SCHOOL_CODE_DUPLICATE", "School code already exists"),
    PACKAGE_NOT_FOUND("ERR_PACKAGE_NOT_FOUND", "Benefit package not found"),
    PERMISSION_NOT_FOUND("ERR_PERMISSION_NOT_FOUND", "Permission not found"),
    SLOT_LIMIT_EXCEEDED("ERR_SLOT_LIMIT_EXCEEDED", "Promotional slot limit (10) exceeded"),
    LOGIN_NAME_DUPLICATE("ERR_LOGIN_NAME_DUPLICATE", "Login name already exists in this tenant"),
    SECONDARY_ROLE_INVALID("ERR_SECONDARY_ROLE_INVALID", "Secondary role is incompatible with primary account type"),
    EMAIL_FORMAT_INVALID("ERR_EMAIL_FORMAT_INVALID", "Email format is invalid"),
    PHONE_FORMAT_INVALID("ERR_PHONE_FORMAT_INVALID", "Phone number format is invalid"),
    SELF_DELETE_FORBIDDEN("ERR_SELF_DELETE_FORBIDDEN", "Cannot delete your own account"),
    FILE_TOO_LARGE("ERR_FILE_TOO_LARGE", "File exceeds maximum row limit of 1000"),
    FILE_FORMAT_INVALID("ERR_FILE_FORMAT_INVALID", "Unsupported file format, use .xlsx or .csv");

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
