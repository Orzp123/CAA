package com.caa.auth.integration;

import com.caa.auth.config.JwtConfig;
import com.caa.auth.dto.TokenInfo;
import com.caa.auth.model.Account;
import com.caa.auth.service.SingleDeviceService;
import com.caa.auth.service.TokenBlacklistService;
import com.caa.auth.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-level integration tests for TokenBlacklistService and SingleDeviceService.
 *
 * <p>These tests use a mocked StringRedisTemplate so no Redis instance is required.
 * They exercise the service contracts directly without a Spring context, which keeps
 * the tests fast and removes infrastructure dependencies.
 */
class TokenBlacklistIntegrationTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private TokenBlacklistService tokenBlacklistService;
    private SingleDeviceService singleDeviceService;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        tokenBlacklistService = new TokenBlacklistService(redisTemplate);

        // SingleDeviceService also needs repo mocks
        var singleDeviceRepo = mock(
                com.caa.auth.repository.TenantSingleDeviceConfigRepository.class);
        var systemConfigRepo = mock(
                com.caa.auth.repository.SystemConfigRepository.class);
        when(singleDeviceRepo.findByTenantIdAndAccountType(anyString(), any()))
                .thenReturn(Optional.empty());
        when(systemConfigRepo.findById(anyString())).thenReturn(Optional.empty());

        singleDeviceService = new SingleDeviceService(
                singleDeviceRepo, systemConfigRepo, redisTemplate);

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-for-token-blacklist-tests-long-enough");
        jwtConfig.setExpirationSeconds(3600);
        tokenService = new TokenService(jwtConfig);
    }

    // ── Test 1: Token added to blacklist is detected as blacklisted ──────────

    @Test
    void addToBlacklist_tokenIsSubsequentlyDetectedAsBlacklisted() {
        String jti = "test-jti-001";
        Duration ttl = Duration.ofHours(1);

        // Before adding: not blacklisted
        when(redisTemplate.hasKey("token:blacklist:" + jti)).thenReturn(false);
        assertThat(tokenBlacklistService.isBlacklisted(jti)).isFalse();

        // Add to blacklist
        tokenBlacklistService.addToBlacklist(jti, ttl);
        verify(valueOps).set("token:blacklist:" + jti, "1", ttl);

        // After adding: blacklisted
        when(redisTemplate.hasKey("token:blacklist:" + jti)).thenReturn(true);
        assertThat(tokenBlacklistService.isBlacklisted(jti)).isTrue();
    }

    // ── Test 2: Token not in blacklist returns false ──────────────────────────

    @Test
    void isBlacklisted_tokenNotInBlacklist_returnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        assertThat(tokenBlacklistService.isBlacklisted("unknown-jti")).isFalse();
    }

    // ── Test 3: Redis returns null — isBlacklisted returns false ─────────────

    @Test
    void isBlacklisted_redisReturnsNull_returnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenReturn(null);
        assertThat(tokenBlacklistService.isBlacklisted("jti-null-redis")).isFalse();
    }

    // ── Test 4: Single-device — new login invalidates old token ──────────────

    @Test
    void singleDevice_newLogin_invalidatesOldJti() {
        String accountId = "account-001";
        String deviceType = "WEB";
        String oldJti = "old-jti-abc";
        String newJti = "new-jti-xyz";
        Duration ttl = Duration.ofHours(2);

        // Simulate existing session: getAndSet returns the old JTI
        when(valueOps.getAndSet(
                eq("token:active:" + accountId + ":" + deviceType),
                eq(newJti)))
                .thenReturn(oldJti);

        Optional<String> displaced = singleDeviceService.trackLogin(
                accountId, deviceType, newJti, ttl);

        assertThat(displaced).isPresent().hasValue(oldJti);

        // Caller (AuthService) would then blacklist the old JTI
        tokenBlacklistService.addToBlacklist(oldJti, ttl);
        verify(valueOps).set("token:blacklist:" + oldJti, "1", ttl);
    }

    // ── Test 5: Single-device — first login returns empty ────────────────────

    @Test
    void singleDevice_firstLogin_returnsEmpty() {
        String accountId = "account-002";
        String deviceType = "WEB";
        Duration ttl = Duration.ofHours(2);

        // No prior session: getAndSet returns null
        when(valueOps.getAndSet(anyString(), anyString())).thenReturn(null);

        Optional<String> displaced = singleDeviceService.trackLogin(
                accountId, deviceType, "jti-first", ttl);

        assertThat(displaced).isEmpty();
    }

    // ── Test 6: TokenService issues a valid JWT that can be parsed back ───────

    @Test
    void tokenService_issueAndParse_roundTrip() {
        TokenInfo info = tokenService.issue(
                "account-003", "tenant-001",
                Account.AccountType.STUDENT, "S12345");

        assertThat(info.token()).isNotBlank();
        assertThat(info.jti()).isNotBlank();
        assertThat(info.expiresAt()).isNotNull();

        var jwt = tokenService.parse(info.token());
        assertThat(jwt.getId()).isEqualTo(info.jti());
        assertThat(jwt.getSubject()).isEqualTo("account-003");
        assertThat(jwt.<String>getClaim("tenantId")).isEqualTo("tenant-001");
        assertThat(jwt.<String>getClaim("accountType")).isEqualTo("STUDENT");
        assertThat(jwt.<String>getClaim("studentNo")).isEqualTo("S12345");
    }
}
