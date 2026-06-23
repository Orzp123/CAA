package com.caa.auth.service.sso;

import java.util.Map;

/**
 * Extracted OIDC user information after a successful token exchange.
 *
 * <p>{@code subject} is the IdP's stable user identifier (the {@code sub} claim).
 * {@code claims} holds the full ID-token / userinfo payload so callers can
 * inspect non-standard claims (e.g. a role claim) without re-parsing.
 */
public record OidcUserInfo(
        String subject,
        String name,
        String email,
        Map<String, Object> claims
) {}
