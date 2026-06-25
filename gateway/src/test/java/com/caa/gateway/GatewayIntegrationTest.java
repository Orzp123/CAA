package com.caa.gateway;

import com.caa.gateway.config.WhitelistConfig;
import com.caa.gateway.filter.AuthenticationFilter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Gateway 集成测试 — 无需外部服务，验证过滤器链行为。
 * US6 验收标准：白名单放行、无 Token → 401、黑名单 Token → 401、有效 Token 通过。
 */
@ExtendWith(MockitoExtension.class)
class GatewayIntegrationTest {

    private static final String TEST_SECRET = "test-secret-key-32-chars-padding!";

    @Mock WhitelistConfig                           whitelistConfig;
    @Mock ReactiveStringRedisTemplate               redisTemplate;
    @Mock ReactiveValueOperations<String, String>   valueOps;
    @Mock GatewayFilterChain                        chain;

    // ── 白名单放行 ─────────────────────────────────────────────────────────────

    @Test
    void whitelistedPath_passesWithoutToken() {
        when(whitelistConfig.getPaths()).thenReturn(List.of("/auth/login", "/auth/captcha/**"));
        when(chain.filter(any())).thenReturn(Mono.empty());

        var filter   = new AuthenticationFilter(whitelistConfig, redisTemplate, TEST_SECRET);
        var request  = MockServerHttpRequest.get("/auth/login").build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .as("白名单路径不得返回 401")
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 无 Token → 401 ────────────────────────────────────────────────────────

    @Test
    void missingToken_returns401() {
        when(whitelistConfig.getPaths()).thenReturn(List.of());

        var filter   = new AuthenticationFilter(whitelistConfig, redisTemplate, TEST_SECRET);
        var request  = MockServerHttpRequest.get("/api/agents").build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 过期 Token → 401 ──────────────────────────────────────────────────────

    @Test
    void expiredToken_returns401() throws Exception {
        when(whitelistConfig.getPaths()).thenReturn(List.of());

        String token = buildToken("acc-1", "tenant-1", Instant.now().minusSeconds(10));
        var filter   = new AuthenticationFilter(whitelistConfig, redisTemplate, TEST_SECRET);
        var request  = MockServerHttpRequest.get("/api/agents")
                .header("Authorization", "Bearer " + token).build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 黑名单 Token → 401 ────────────────────────────────────────────────────

    @Test
    void blacklistedToken_returns401() throws Exception {
        when(whitelistConfig.getPaths()).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(Mono.just("revoked"));

        String token = buildToken("acc-1", "tenant-1", Instant.now().plusSeconds(3600));
        var filter   = new AuthenticationFilter(whitelistConfig, redisTemplate, TEST_SECRET);
        var request  = MockServerHttpRequest.get("/api/agents")
                .header("Authorization", "Bearer " + token).build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 有效 Token 通过过滤器 ──────────────────────────────────────────────────

    @Test
    void validToken_notBlacklisted_passesThrough() throws Exception {
        when(whitelistConfig.getPaths()).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(Mono.empty());
        when(chain.filter(any())).thenReturn(Mono.empty());

        String token = buildToken("acc-1", "tenant-1", Instant.now().plusSeconds(3600));
        var filter   = new AuthenticationFilter(whitelistConfig, redisTemplate, TEST_SECRET);
        var request  = MockServerHttpRequest.get("/api/agents")
                .header("Authorization", "Bearer " + token).build();
        var exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .as("有效 Token 不应返回 401/403")
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildToken(String accountId, String tenantId, Instant expiry) throws Exception {
        byte[] secret = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(accountId)
                .claim("tenantId", tenantId)
                .claim("jti", UUID.randomUUID().toString())
                .expirationTime(Date.from(expiry))
                .issueTime(Date.from(Instant.now()))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret));
        return jwt.serialize();
    }
}
