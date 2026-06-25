package com.caa.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Resolves the current tenant before authentication.
 *
 * Resolution order:
 *   1. Subdomain of Host header: {tenantCode}.caa.example.com
 *   2. X-School-Code request header
 *
 * Cache key: tenant:domain:{tenantCode}  →  tenantId (String)
 * If tenant not found, request continues without X-Tenant-Id header
 * (AuthenticationFilter will reject protected paths if tenantId is absent).
 */
@Component
public class TenantResolutionFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantResolutionFilter.class);

    static final String CACHE_KEY_PREFIX = "tenant:domain:";
    static final Duration CACHE_TTL = Duration.ofMinutes(5);
    static final String HEADER_TENANT_ID = "X-Tenant-Id";
    static final String HEADER_SCHOOL_CODE = "X-School-Code";

    private final ReactiveStringRedisTemplate redisTemplate;

    public TenantResolutionFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantCode = extractTenantCode(exchange);

        if (tenantCode == null) {
            return chain.filter(exchange);
        }

        String cacheKey = CACHE_KEY_PREFIX + tenantCode;

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .defaultIfEmpty("")
                .flatMap(tenantId -> {
                    if (tenantId.isEmpty()) {
                        log.debug("Tenant not found in cache for code: {}", tenantCode);
                        return chain.filter(exchange);
                    }
                    return chain.filter(withTenantId(exchange, tenantId));
                })
                .onErrorResume(ex -> {
                    log.warn("Redis error during tenant resolution, proceeding without tenant: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    /**
     * Extract tenant code from subdomain first, then X-School-Code header.
     */
    private String extractTenantCode(ServerWebExchange exchange) {
        // 1. Try Host header subdomain
        String host = exchange.getRequest().getHeaders().getFirst("Host");
        if (host != null) {
            // Strip port if present
            String hostname = host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
            String[] parts = hostname.split("\\.", 2);
            if (parts.length == 2 && !parts[0].isEmpty()) {
                // Only treat as tenant subdomain if there is a dot (i.e. not bare IP / localhost)
                return parts[0];
            }
        }

        // 2. Fallback: X-School-Code header
        String schoolCode = exchange.getRequest().getHeaders().getFirst(HEADER_SCHOOL_CODE);
        if (schoolCode != null && !schoolCode.isBlank()) {
            return schoolCode.trim();
        }

        return null;
    }

    /**
     * Return a mutated exchange with X-Tenant-Id added to the downstream request.
     * Package-private to allow direct unit testing of header mutation.
     */
    ServerWebExchange withTenantIdForTest(ServerWebExchange exchange, String tenantId) {
        return withTenantId(exchange, tenantId);
    }

    private ServerWebExchange withTenantId(ServerWebExchange exchange, String tenantId) {
        ServerHttpRequest mutated = exchange.getRequest()
                .mutate()
                .header(HEADER_TENANT_ID, tenantId)
                .build();
        return exchange.mutate().request(mutated).build();
    }
}
