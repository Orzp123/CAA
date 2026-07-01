package com.caa.school.dto;

import java.time.LocalDateTime;

/**
 * 创建学校响应 DTO。
 * defaultAdminLoginName = "admin_" + code
 */
public record CreateSchoolResponse(
        String schoolId,
        String code,
        String name,
        String defaultAdminLoginName,
        String status,
        LocalDateTime createdAt
) {}
