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
) {
    /** 返回不含 token 的副本，用于 HTTP 响应 body（token 已通过 Set-Cookie 发送）。
     *
     * <p>参数顺序与 record 声明一致：
     * token=null, expiresAt, accountId, nickname, accountType, tenantId, tenantName
     */
    public LoginResponse withoutToken() {
        return new LoginResponse(
                null,           // token — 不向客户端暴露，已通过 Set-Cookie 发送
                expiresAt,      // expiresAt
                accountId,      // accountId
                nickname,       // nickname
                accountType,    // accountType
                tenantId,       // tenantId
                tenantName      // tenantName
        );
    }
}
