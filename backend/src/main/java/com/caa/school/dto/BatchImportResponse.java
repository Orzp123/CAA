package com.caa.school.dto;

/**
 * 批量导入账户响应 DTO。
 * reportDownloadUrl 为 MinIO 预签名下载地址。
 */
public record BatchImportResponse(
        int total,
        int successCount,
        int failureCount,
        String reportDownloadUrl
) {}
