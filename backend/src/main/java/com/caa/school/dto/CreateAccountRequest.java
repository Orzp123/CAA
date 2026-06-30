package com.caa.school.dto;

import jakarta.validation.constraints.Size;

/**
 * 创建账户请求 DTO。
 * password 为 null 时使用默认密码 "Caa@2026"。
 */
public record CreateAccountRequest(
        String loginName,
        String name,
        String accountType,
        String secondaryRole,
        String nickname,
        String email,
        String phone,
        @Size(min = 8, max = 128, message = "密码长度须在 8 到 128 位之间")
        String password
) {}
