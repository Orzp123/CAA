package com.caa.auth.dto;

import java.util.List;

/**
 * Response body for GET /auth/tenant-config.
 *
 * @param tenantId            unique tenant identifier
 * @param tenantName          display name of the tenant
 * @param defaultLoginType    default login method (e.g. "PASSWORD", "WECHAT", "SSO")
 * @param availableLoginTypes all login methods available for this tenant
 * @param logoUrl             optional URL to the tenant's logo image
 */
public record TenantConfigResponse(
        String tenantId,
        String tenantName,
        String defaultLoginType,
        List<String> availableLoginTypes,
        String logoUrl
) {}
