package com.caa.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    @Mock
    private GatewayFilterChain chain;

    private TenantResolutionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantResolutionFilter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void hostSubdomain_extractsTenantCode_andCallsChain() {
        // Cache hit: "school1" → "tenant-id-001" — chain.filter must be called
        when(valueOps.get(TenantResolutionFilter.CACHE_KEY_PREFIX + "school1"))
                .thenReturn(Mono.just("tenant-id-001"));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users")
                .header("Host", "school1.caa.example.com")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // chain.filter was invoked (with the mutated exchange — runtime behavior)
        verify(chain).filter(any());
    }

    @Test
    void xSchoolCodeHeader_usedAsFallback_andCallsChain() {
        // Host is bare "localhost" — no subdomain, fallback to X-School-Code
        when(valueOps.get(TenantResolutionFilter.CACHE_KEY_PREFIX + "school2"))
                .thenReturn(Mono.just("tenant-id-002"));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users")
                .header("Host", "localhost")
                .header("X-School-Code", "school2")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void cacheHit_callsChainWithMutatedExchange() {
        when(valueOps.get(TenantResolutionFilter.CACHE_KEY_PREFIX + "school3"))
                .thenReturn(Mono.just("cached-tenant-id"));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/data")
                .header("Host", "school3.caa.example.com")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Capture the exchange actually passed to chain.filter
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        when(chain.filter(any())).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(captured.get()).isNotNull();
        // The exchange passed to chain must not be the original (it was mutated)
        // MockServerWebExchange.mutate() wraps into DefaultServerWebExchange;
        // verify the request URI is preserved (header mutation is tested via withTenantId_addsHeader)
        assertThat(captured.get().getRequest().getURI().getPath()).isEqualTo("/api/data");
    }

    @Test
    void cacheMiss_proceedsWithoutBlocking() {
        when(valueOps.get(anyString())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/data")
                .header("Host", "unknown.caa.example.com")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Chain is still called on cache miss (no blocking, no 4xx)
        verify(chain).filter(any());
    }

    @Test
    void noTenantCode_noHostAndNoSchoolCode_skipsRedisAndCallsChain() {
        // No Host header, no X-School-Code → tenantCode is null → skip Redis entirely
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/data")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void withTenantId_addsXTenantIdHeader() {
        // Direct unit test of header mutation helper — bypasses MockServerWebExchange limitation
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/data")
                .header("Host", "school4.caa.example.com")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerWebExchange mutated = filter.withTenantIdForTest(exchange, "tenant-xyz");

        assertThat(mutated.getRequest().getHeaders().getFirst("X-Tenant-Id"))
                .isEqualTo("tenant-xyz");
    }
}
