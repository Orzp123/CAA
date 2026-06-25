package com.caa.auth.service.wechat;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WechatOAuth2Service}.
 * No Spring context — all dependencies mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WechatOAuth2ServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private RestClient restClient;

    // RestClient fluent chain mocks
    @Mock
    private RestClient.RequestHeadersUriSpec<?> uriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private WechatOAuth2Service service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        WechatConfig config = new WechatConfig();
        config.setAppid("wx_appid_test");
        config.setSecret("wx_secret_test");

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new WechatOAuth2Service(
                config,
                accountRepository,
                tenantRepository,
                redisTemplate,
                restClient
        );
    }

    // ── exchangeCode ─────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void exchangeCode_returnsTokenResult_withOpenid_whenWechatSucceeds() {
        WechatTokenResult expected = new WechatTokenResult(
                "ACCESS_TOKEN_XYZ", "OPENID_123", "UNIONID_456", null, null);

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) uriSpec);
        when(uriSpec.uri(anyString())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WechatTokenResult.class)).thenReturn(expected);

        WechatTokenResult result = service.exchangeCode("AUTH_CODE_ABC");

        assertThat(result).isNotNull();
        assertThat(result.openid()).isEqualTo("OPENID_123");
        assertThat(result.unionid()).isEqualTo("UNIONID_456");
        assertThat(result.accessToken()).isEqualTo("ACCESS_TOKEN_XYZ");
        assertThat(result.isSuccess()).isTrue();
    }

    // ── findOrCreateAccount ───────────────────────────────────────────────────

    @Test
    void findOrCreateAccount_returnsExistingAccount_whenOpenidFound() {
        Account existing = buildAccount("tenant-wechat-1", "OPENID_123");
        when(accountRepository.findByWechatOpenid("OPENID_123"))
                .thenReturn(Optional.of(existing));

        Account result = service.findOrCreateAccount("OPENID_123", "UNIONID_456");

        assertThat(result).isSameAs(existing);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void findOrCreateAccount_throwsRequiresProfileCompletionException_whenOpenidNotFound() {
        when(accountRepository.findByWechatOpenid("NEW_OPENID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOrCreateAccount("NEW_OPENID", null))
                .isInstanceOf(RequiresProfileCompletionException.class)
                .hasMessageContaining("NEW_OPENID");
    }

    // ── generateTempToken ─────────────────────────────────────────────────────

    @Test
    void generateTempToken_storesOpenidInRedis_andReturnsUuid() {
        String openid = "OPENID_TEMP_999";

        String token = service.generateTempToken(openid);

        assertThat(token).isNotBlank();
        // UUID format: 8-4-4-4-12
        assertThat(token).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), valCaptor.capture(),
                eq(10L), eq(TimeUnit.MINUTES));

        assertThat(keyCaptor.getValue()).isEqualTo("wechat:temp:" + token);
        assertThat(valCaptor.getValue()).isEqualTo(openid);
    }

    // ── completeProfile ───────────────────────────────────────────────────────

    @Test
    void completeProfile_createsNewAccount_inWechatTenant() {
        String tempToken = "some-temp-uuid";
        String openid = "OPENID_NEW";
        Tenant wechatTenant = buildTenant("tenant-wechat-id", "wechat");

        when(valueOps.get("wechat:temp:" + tempToken)).thenReturn(openid);
        when(tenantRepository.findByCode("wechat")).thenReturn(Optional.of(wechatTenant));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.completeProfile(
                tempToken, "S2024001", "张三", "小三", Account.AccountType.STUDENT);

        assertThat(result).isNotNull();
        Account saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-wechat-id");
        assertThat(saved.getStudentNo()).isEqualTo("S2024001");
        assertThat(saved.getName()).isEqualTo("张三");
        assertThat(saved.getNickname()).isEqualTo("小三");
        assertThat(saved.getAccountType()).isEqualTo(Account.AccountType.STUDENT);
        assertThat(saved.getWechatOpenid()).isEqualTo(openid);
        assertThat(saved.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Account buildAccount(String tenantId, String openid) {
        Account a = new Account();
        a.setTenantId(tenantId);
        a.setWechatOpenid(openid);
        a.setStudentNo("S001");
        a.setName("Test User");
        a.setAccountType(Account.AccountType.STUDENT);
        a.setStatus(Account.AccountStatus.ACTIVE);
        return a;
    }

    private Tenant buildTenant(String id, String code) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setCode(code);
        t.setName("WeChat Tenant");
        t.setType(Tenant.TenantType.WECHAT);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        return t;
    }
}
