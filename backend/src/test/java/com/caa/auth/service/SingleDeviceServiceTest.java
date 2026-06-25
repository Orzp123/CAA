package com.caa.auth.service;

import com.caa.auth.model.Account.AccountType;
import com.caa.auth.model.SystemConfig;
import com.caa.auth.model.TenantSingleDeviceConfig;
import com.caa.auth.repository.SystemConfigRepository;
import com.caa.auth.repository.TenantSingleDeviceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SingleDeviceServiceTest {

    @Mock
    private TenantSingleDeviceConfigRepository tenantSingleDeviceConfigRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private SingleDeviceService service;

    private static final String TENANT_ID = "tenant-001";
    private static final AccountType ACCOUNT_TYPE = AccountType.STUDENT;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new SingleDeviceService(
                tenantSingleDeviceConfigRepository,
                systemConfigRepository,
                redisTemplate);
    }

    // ── isEnabled ────────────────────────────────────────────────────────────

    @Test
    void isEnabled_returnsAccountTypeLevelConfig_whenPresent() {
        TenantSingleDeviceConfig config = buildConfig(TENANT_ID, ACCOUNT_TYPE, true);
        when(tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(TENANT_ID, ACCOUNT_TYPE))
                .thenReturn(Optional.of(config));

        assertThat(service.isEnabled(TENANT_ID, ACCOUNT_TYPE)).isTrue();
        // tenant-level and global should not be queried
        verify(tenantSingleDeviceConfigRepository, never())
                .findByTenantIdAndAccountType(TENANT_ID, null);
        verify(systemConfigRepository, never()).findById(any());
    }

    @Test
    void isEnabled_fallsBackToTenantLevel_whenAccountTypeConfigAbsent() {
        when(tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(TENANT_ID, ACCOUNT_TYPE))
                .thenReturn(Optional.empty());
        TenantSingleDeviceConfig tenantConfig = buildConfig(TENANT_ID, null, false);
        when(tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(TENANT_ID, null))
                .thenReturn(Optional.of(tenantConfig));

        assertThat(service.isEnabled(TENANT_ID, ACCOUNT_TYPE)).isFalse();
        verify(systemConfigRepository, never()).findById(any());
    }

    @Test
    void isEnabled_fallsBackToGlobalSystemConfig_whenBothTenantConfigsAbsent() {
        when(tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(TENANT_ID, ACCOUNT_TYPE))
                .thenReturn(Optional.empty());
        when(tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(TENANT_ID, null))
                .thenReturn(Optional.empty());
        SystemConfig globalConfig = buildSystemConfig("single_device.enabled", "true");
        when(systemConfigRepository.findById("single_device.enabled"))
                .thenReturn(Optional.of(globalConfig));

        assertThat(service.isEnabled(TENANT_ID, ACCOUNT_TYPE)).isTrue();
    }

    @Test
    void isEnabled_returnsFalse_whenNoConfigAtAnyLevel() {
        when(tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(TENANT_ID, ACCOUNT_TYPE))
                .thenReturn(Optional.empty());
        when(tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(TENANT_ID, null))
                .thenReturn(Optional.empty());
        when(systemConfigRepository.findById("single_device.enabled"))
                .thenReturn(Optional.empty());

        assertThat(service.isEnabled(TENANT_ID, ACCOUNT_TYPE)).isFalse();
    }

    // ── trackLogin ───────────────────────────────────────────────────────────

    @Test
    void trackLogin_storesNewJtiAndReturnsOldJti_whenPreviousExists() {
        String accountId = "acct-001";
        String deviceType = "WEB";
        String oldJti = "old-jti-111";
        String newJti = "new-jti-222";
        Duration ttl = Duration.ofHours(1);
        String key = "token:active:acct-001:WEB";

        when(valueOps.getAndSet(key, newJti)).thenReturn(oldJti);

        Optional<String> result = service.trackLogin(accountId, deviceType, newJti, ttl);

        assertThat(result).contains(oldJti);
        verify(valueOps).getAndSet(key, newJti);
        verify(redisTemplate).expire(key, ttl);
    }

    @Test
    void trackLogin_returnsEmpty_whenNoPreviousJtiExists() {
        String accountId = "acct-002";
        String deviceType = "MOBILE";
        String newJti = "new-jti-333";
        Duration ttl = Duration.ofHours(2);
        String key = "token:active:acct-002:MOBILE";

        when(valueOps.getAndSet(key, newJti)).thenReturn(null);

        Optional<String> result = service.trackLogin(accountId, deviceType, newJti, ttl);

        assertThat(result).isEmpty();
        verify(valueOps).getAndSet(key, newJti);
        verify(redisTemplate).expire(key, ttl);
    }

    // ── getActiveJti ─────────────────────────────────────────────────────────

    @Test
    void getActiveJti_returnsJti_whenKeyPresent() {
        when(valueOps.get("token:active:acct-001:WEB")).thenReturn("jti-abc");

        Optional<String> result = service.getActiveJti("acct-001", "WEB");

        assertThat(result).contains("jti-abc");
    }

    @Test
    void getActiveJti_returnsEmpty_whenKeyAbsent() {
        when(valueOps.get("token:active:acct-001:WEB")).thenReturn(null);

        Optional<String> result = service.getActiveJti("acct-001", "WEB");

        assertThat(result).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TenantSingleDeviceConfig buildConfig(String tenantId, AccountType accountType, boolean enabled) {
        TenantSingleDeviceConfig c = new TenantSingleDeviceConfig();
        c.setTenantId(tenantId);
        c.setAccountType(accountType);
        c.setEnabled(enabled);
        return c;
    }

    private SystemConfig buildSystemConfig(String key, String value) {
        SystemConfig sc = new SystemConfig();
        sc.setConfigKey(key);
        sc.setConfigValue(value);
        return sc;
    }
}
