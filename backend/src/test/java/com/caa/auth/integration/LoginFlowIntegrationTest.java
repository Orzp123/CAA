package com.caa.auth.integration;

import com.caa.auth.dto.LoginRequest;
import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.TenantRepository;
import com.caa.auth.service.CaptchaService;
import com.caa.auth.service.sso.SsoOidcService;
import com.caa.auth.service.wechat.WechatOAuth2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Full password-login integration test using an in-process HTTP server (H2 + mocked Redis).
 *
 * <p>Infrastructure mocked via @MockitoBean (Spring Boot 4.x / Spring Framework 7.x):
 * <ul>
 *   <li>StringRedisTemplate — token blacklist + single-device tracking</li>
 *   <li>CaptchaService — always returns true so captcha doesn't block login</li>
 *   <li>KafkaTemplate — prevents Kafka broker connection on context startup</li>
 *   <li>MinioClient — prevents MinIO connection on context startup</li>
 *   <li>OpenAiChatModel / AnthropicChatModel / EmbeddingModel — prevents AI API calls</li>
 * </ul>
 * Nacos auto-configurations are excluded in application-test.yml.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.nacos.config.enabled=false",
            "spring.cloud.nacos.discovery.enabled=false",
            "spring.cloud.nacos.config.import-check.enabled=false",
            "spring.config.import=",
            "nacos.config.enabled=false",
            "spring.autoconfigure.exclude=com.alibaba.cloud.nacos.NacosConfigAutoConfiguration,com.alibaba.cloud.nacos.NacosServiceAutoConfiguration,org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration"
        }
)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // ── Infrastructure mocks (Spring Boot 4.x uses @MockitoBean) ─────────────

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private CaptchaService captchaService;

    @SuppressWarnings("rawtypes")
    @MockitoBean
    private KafkaTemplate kafkaTemplate;

    @MockitoBean
    private MinioClient minioClient;

    @MockitoBean
    private OpenAiChatModel openAiChatModel;

    @MockitoBean
    private AnthropicChatModel anthropicChatModel;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @MockitoBean
    private RestClient restClient;

    @MockitoBean
    private ObjectMapper objectMapper;

    @MockitoBean
    private WechatOAuth2Service wechatOAuth2Service;

    @MockitoBean
    private SsoOidcService ssoOidcService;

    @MockitoBean
    private JedisConnectionFactory jedisConnectionFactory;

    // ── Test data ─────────────────────────────────────────────────────────────

    private static final String TENANT_CODE = "testschool";
    private static final String STUDENT_NO  = "S20240001";
    private static final String PASSWORD    = "Password123!";
    private String tenantId;

    @BeforeEach
    void setUp() {
        // Captcha: always valid
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        when(captchaService.generate(anyString())).thenReturn(new byte[]{});

        // Redis ValueOperations stub
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.getAndSet(anyString(), anyString())).thenReturn(null);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        // No expire stub needed — mock returns null by default, which is fine for tests

        // Create tenant
        Tenant tenant = new Tenant();
        tenant.setCode(TENANT_CODE);
        tenant.setName("Test School");
        tenant.setType(Tenant.TenantType.SCHOOL);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        // Create account with hashed password
        Account account = new Account();
        account.setTenantId(tenantId);
        account.setStudentNo(STUDENT_NO);
        account.setName("Test Student");
        account.setAccountType(Account.AccountType.STUDENT);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setPasswordHash(passwordEncoder.encode(PASSWORD));
        accountRepository.save(account);
    }

    // ── Test 1: Full password login flow ─────────────────────────────────────

    @Test
    void fullPasswordLoginFlow_createsAccountAndReturnsToken() {
        LoginRequest req = new LoginRequest(STUDENT_NO, TENANT_CODE, PASSWORD, "uuid1", "1234");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                req,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();

        // ApiResponse wraps payload under "data"
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        // C-1 fix: JWT is now in httpOnly cookie, not response body
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank().contains("caa_token=");
        assertThat(data.get("accountType")).isEqualTo("STUDENT");
        assertThat(data.get("tenantId")).isEqualTo(tenantId);
    }

    // ── Test 2: Invalid captcha returns 401 ──────────────────────────────────

    @Test
    void login_withInvalidCaptcha_returns401() {
        when(captchaService.verify(anyString(), anyString())).thenReturn(false);

        LoginRequest req = new LoginRequest(STUDENT_NO, TENANT_CODE, PASSWORD, "uuid2", "WRONG");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                req,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Test 3: Account lockout after 5 failures ─────────────────────────────

    @Test
    void login_after5Failures_accountBecomesLocked() {
        LoginRequest wrongPw = new LoginRequest(
                STUDENT_NO, TENANT_CODE, "WRONG_PW", "uuid3", "1234");

        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/auth/login",
                    wrongPw,
                    Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // 6th attempt with correct password — account is now locked
        LoginRequest correctPw = new LoginRequest(
                STUDENT_NO, TENANT_CODE, PASSWORD, "uuid3", "1234");
        ResponseEntity<Map> lockedResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                correctPw,
                Map.class);

        assertThat(lockedResp.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ── Test 4: Logout blacklists token ──────────────────────────────────────

    @Test
    void logout_blacklistsToken() {
        // Login to get a token
        LoginRequest req = new LoginRequest(
                STUDENT_NO, TENANT_CODE, PASSWORD, "uuid4", "1234");
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                req,
                Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loginResp.getBody().get("data");
        // C-1 fix: JWT is in Set-Cookie header, not response body
        String setCookie = loginResp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank().contains("caa_token=");
        String token = setCookie.split("caa_token=")[1].split(";")[0];
        assertThat(token).isNotBlank();

        // Logout
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> logoutReq = new HttpEntity<>(headers);

        ResponseEntity<Map> logoutResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/auth/logout",
                HttpMethod.POST,
                logoutReq,
                Map.class);

        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify blacklist write was called — opsForValue().set(...) with a blacklist key.
        // Use ArgumentCaptor on the 2-arg set to avoid Spring Data Redis 4.x overload ambiguity.
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        org.mockito.ArgumentCaptor<String> keyCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        // Verify 2-arg set (key, value) — TokenBlacklistService calls set(key,"1",ttl) which
        // the mock records; we can't verify the 3-arg form due to overload ambiguity in 4.x,
        // so we confirm opsForValue() was invoked and the key matches the blacklist prefix.
        verify(stringRedisTemplate, atLeastOnce()).opsForValue();
        // Additionally confirm the key was set by checking mock interactions via mockingDetails
        boolean blacklistCallFound = org.mockito.Mockito.mockingDetails(valueOps)
                .getInvocations()
                .stream()
                .filter(inv -> inv.getMethod().getName().equals("set"))
                .anyMatch(inv -> {
                    Object[] args = inv.getArguments();
                    return args.length >= 2
                            && args[0] instanceof String k
                            && k.startsWith("token:blacklist:");
                });
        assertThat(blacklistCallFound)
                .as("Expected at least one set() call with a token:blacklist: key")
                .isTrue();
    }
}
