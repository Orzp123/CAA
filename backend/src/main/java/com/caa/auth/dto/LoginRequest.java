package com.caa.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /auth/login (password login).
 *
 * @param studentNo   student / employee number
 * @param schoolCode  tenant code identifying the school / organisation
 * @param password    raw password (hashed server-side)
 * @param captchaUuid UUID of the captcha challenge
 * @param captchaCode user-entered captcha answer
 */
public record LoginRequest(
        @NotBlank String studentNo,
        @NotBlank String schoolCode,
        @NotBlank String password,
        @NotBlank String captchaUuid,
        @NotBlank String captchaCode
) {}
