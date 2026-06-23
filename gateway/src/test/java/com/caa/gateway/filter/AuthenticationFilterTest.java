package com.caa.gateway.filter;

import com.caa.gateway.config.WhitelistConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private static final String TEST_SECRET = "test-secret-key-32-chars-padding!";

    @Mock
    private WhitelistConfig whitelistConfig;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private GatewayFilterChain chain;

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(whitelistConfig, redisTemplate, TEST_SECRET);
    }

    // ── helper: build a signed JWT ───────────────────────────────────────────

    private String buildToken(String subject, String jti, Instant expiry, String accountType) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .jwtID(jti)
                .expirationTime(Date.from(expiry));
        if (accountType != null) {
            claims.claim("accountType", accountType);
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims.build()
        );
        jwt.sign(new MACSigner(TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    void whitelistedPath_passesWithoutToken() {
        when(whitelistConfig.isWhitelisted("/auth/login")).thenReturn(true);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // chain.filter called (Mockito verifies via when() default Mono.empty())
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void validToken_nonBlacklisted_passesAndInjectsClaims() throws Exception {
        when(whitelistConfig.isWhitelisted(anyString())).thenReturn(false);

        String jti = UUID.randomUUID().toString();
        String token = buildToken("user-123", jti, Instant.now().plusSeconds(3600), "TEACHER");

        when(redisTemplate.hasKey("token:blacklist:" + jti)).thenReturn(Mono.just(false));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/courses")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerWebExchange[] captured = new ServerWebExchange[1];
        when(chain.filter(any())).thenAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(captured[0].getRequest().getHeaders().getFirst("X-Account-Id")).isEqualTo("user-123");
        assertThat(captured[0].getRequest().getHeaders().getFirst("X-Account-Type")).isEqualTo("TEACHER");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void blacklistedJti_returns401() throws Exception {
        when(whitelistConfig.isWhitelisted(anyString())).thenReturn(false);

        String jti = UUID.randomUUID().toString();
        String token = buildToken("user-456", jti, Instant.now().plusSeconds(3600), null);

        when(redisTemplate.hasKey("token:blacklist:" + jti)).thenReturn(Mono.just(true));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/courses")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void missingToken_onProtectedPath_returns401() {
        when(whitelistConfig.isWhitelisted(anyString())).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/courses")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void expiredToken_returns401() throws Exception {
        when(whitelistConfig.isWhitelisted(anyString())).thenReturn(false);

        String jti = UUID.randomUUID().toString();
        // Token expired 1 hour ago
        String token = buildToken("user-789", jti, Instant.now().minusSeconds(3600), null);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/courses")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void invalidToken_returns401() {
        when(whitelistConfig.isWhitelisted(anyString())).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/courses")
                .header("Authorization", "Bearer not.a.valid.jwt")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }
}
