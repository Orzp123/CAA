package com.caa.auth.dto;

import com.caa.auth.model.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /auth/wechat/complete-profile.
 *
 * @param tempToken   short-lived token issued during first WeChat login
 * @param studentNo   student / employee number
 * @param name        real name
 * @param nickname    display name
 * @param accountType role in the system
 */
public record CompleteProfileRequest(
        @NotBlank String tempToken,
        @NotBlank String studentNo,
        @NotBlank String name,
        @NotBlank String nickname,
        @NotNull  Account.AccountType accountType
) {}
