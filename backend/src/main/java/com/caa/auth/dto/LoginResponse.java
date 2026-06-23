package com.caa.auth.dto;

import com.caa.auth.model.Account;
import java.time.Instant;

public record LoginResponse(
        String token,
        Instant expiresAt,
        String accountId,
        String nickname,
        Account.AccountType accountType,
        String tenantId,
        String tenantName
) {}
