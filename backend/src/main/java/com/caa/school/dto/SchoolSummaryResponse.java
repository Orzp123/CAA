package com.caa.school.dto;

import java.time.LocalDateTime;

/**
 * 学校列表摘要响应 DTO。
 */
public record SchoolSummaryResponse(
        String id,
        String code,
        String name,
        String status,
        long adminCount,
        LocalDateTime createdAt
) {}
