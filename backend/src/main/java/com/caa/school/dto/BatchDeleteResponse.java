package com.caa.school.dto;

/**
 * 批量删除账户响应 DTO。
 * excludedSelfCount：因自身保护逻辑被跳过的数量。
 */
public record BatchDeleteResponse(
        int deletedCount,
        int excludedSelfCount
) {}
