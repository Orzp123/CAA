package com.caa.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit test for WhitelistConfig — no Spring context needed.
 */
class WhitelistConfigTest {

    private WhitelistConfig config;

    @BeforeEach
    void setUp() {
        config = new WhitelistConfig();
        config.setWhitelist(List.of(
                "/auth/login",
                "/auth/register",
                "/auth/sso/*/authorize",
                "/actuator/health"
        ));
    }

    @Test
    void exactPathMatch_returnsTrue() {
        assertTrue(config.isWhitelisted("/auth/login"));
        assertTrue(config.isWhitelisted("/actuator/health"));
    }

    @Test
    void wildcardPath_matches() {
        assertTrue(config.isWhitelisted("/auth/sso/github/authorize"));
        assertTrue(config.isWhitelisted("/auth/sso/wechat/authorize"));
    }

    @Test
    void nonWhitelistedPath_returnsFalse() {
        assertFalse(config.isWhitelisted("/api/users"));
        assertFalse(config.isWhitelisted("/auth/sso/github/callback"));
        assertFalse(config.isWhitelisted("/admin/dashboard"));
    }

    @Test
    void nullPath_returnsFalse() {
        assertFalse(config.isWhitelisted(null));
    }
}
