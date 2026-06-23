package com.caa.gateway.filter;

import com.caa.gateway.config.WhitelistConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Authenticates incoming requests by verifying Bearer JWT tokens.
 *
 * Order -90 runs after TenantResolutionFilter (-100).
 *
 * Logic:
 *   1. Skip whitelisted paths
 *   2. Extract Bearer token from Authorization header
 *   3. Verify JWT signature with HS256 secret
 *   4. Check blacklist: token:blacklist:{jti} in Redis
 *   5. On success: propagate X-Account-Id, X-Account-Type to downstream
 *   6. On any failure: return 401 JSON
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WhitelistConfig whitelistConfig;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final JwtDecoder jwtDecoder;

    public AuthenticationFilter(
            WhitelistConfig whitelistConfig,
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${jwt.secret:default-secret-key-32-chars!!}") String jwtSecret) {
        this.whitelistConfig = whitelistConfig;
        this.redisTemplate = redisTemplate;
        this.jwtDecoder = buildDecoder(jwtSecret);
    }

    @Override
    public int getOrder() {
        return -90;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (whitelistConfig.isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        org.springframework.security.oauth2.jwt.Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException e) {
            log.debug("JWT decode failed: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }

        String jti = jwt.getId();
        if (jti == null) {
            return unauthorized(exchange, "Token missing jti claim");
        }

        return redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return unauthorized(exchange, "Token has been revoked");
                    }
                    return chain.filter(withClaims(exchange, jwt));
                })
                .onErrorResume(ex -> {
                    log.error("Redis error during blacklist check: {}", ex.getMessage());
                    // Fail-open on Redis error to avoid full outage; log at ERROR level
                    return chain.filter(withClaims(exchange, jwt));
                });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ServerWebExchange withClaims(
            ServerWebExchange exchange,
            org.springframework.security.oauth2.jwt.Jwt jwt) {

        String accountId = jwt.getSubject();
        String accountType = jwt.getClaimAsString("accountType");

        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        if (accountId != null) {
            builder.header("X-Account-Id", accountId);
        }
        if (accountType != null) {
            builder.header("X-Account-Type", accountType);
        }
        return exchange.mutate().request(builder.build()).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body;
        try {
            body = MAPPER.writeValueAsBytes(Map.of("code", 401, "message", message));
        } catch (JsonProcessingException e) {
            body = "{\"code\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    private static JwtDecoder buildDecoder(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(keySpec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
