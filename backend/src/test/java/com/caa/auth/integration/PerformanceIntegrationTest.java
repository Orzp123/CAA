package com.caa.auth.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Performance integration tests — require running infrastructure.
 *
 * <p>TODO: Run these manually against a live environment with the following setup:
 * <ul>
 *   <li>MySQL 8.4 with the full schema loaded</li>
 *   <li>Redis 7 running at localhost:6379</li>
 *   <li>At least 1000 pre-seeded accounts for load testing</li>
 * </ul>
 *
 * <p>Recommended tooling: Gatling or JMeter for HTTP throughput,
 * or a custom @SpringBootTest with concurrent CompletableFuture submissions.
 *
 * <p>Target SLOs:
 * <ul>
 *   <li>p99 login latency &lt; 500 ms under 100 concurrent users</li>
 *   <li>Token issuance throughput &gt; 500 req/s on a 4-core host</li>
 *   <li>Redis blacklist lookup p99 &lt; 5 ms</li>
 * </ul>
 */
@Disabled("Performance tests require live infrastructure — run manually")
class PerformanceIntegrationTest {

    @Test
    @Disabled
    void loginThroughput_100ConcurrentUsers_p99Under500ms() {
        // TODO: implement with Gatling simulation or JMeter test plan
        // Steps:
        // 1. Seed 1000 accounts across 10 tenants
        // 2. Fire 100 concurrent login requests using ExecutorService
        // 3. Collect latency percentiles with Micrometer or manual timing
        // 4. Assert p99 < 500ms, p50 < 100ms
    }

    @Test
    @Disabled
    void tokenBlacklistLookup_highVolume_p99Under5ms() {
        // TODO: implement against a real Redis instance
        // Steps:
        // 1. Pre-populate 10_000 JTI entries in Redis blacklist
        // 2. Run 1000 isBlacklisted() lookups in parallel
        // 3. Assert p99 latency < 5ms
    }
}
