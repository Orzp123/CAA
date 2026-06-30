package com.caa.school.dto;

import jakarta.validation.constraints.Size;

/**
 * 重置密码请求 DTO。
 * password 为 null 时重置为默认密码 "Caa@2026"。
 */
public record ResetPasswordRequest(
        @Size(min = 8, max = 128, message = "密码长度须在 8 到 128 位之间")
        String password
) {}
