package com.caa.school.dto;

/**
 * Logo 上传预签名 URL 响应 DTO。
 */
public record LogoUploadUrlResponse(
        String uploadUrl,
        String logoPath,
        int expiresIn
) {}
