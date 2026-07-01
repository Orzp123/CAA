package com.caa.school.dto;

import java.util.List;

/**
 * 批量更新账户状态请求 DTO。
 */
public record BatchStatusRequest(
        List<String> accountIds,
        String status
) {}
