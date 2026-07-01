package com.caa.school.dto;

import java.time.LocalDateTime;

/**
 * 账户信息响应 DTO。
 */
public record AccountResponse(
        String id,
        String loginName,
        String name,
        String accountType,
        String secondaryRole,
        String status,
        LocalDateTime createdAt
) {}
