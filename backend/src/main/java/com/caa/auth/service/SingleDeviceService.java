package com.caa.auth.service;

import com.caa.auth.model.Account.AccountType;
import com.caa.auth.repository.SystemConfigRepository;
import com.caa.auth.repository.TenantSingleDeviceConfigRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class SingleDeviceService {

    private static final String ACTIVE_KEY_PREFIX = "token:active:";
    private static final String GLOBAL_CONFIG_KEY = "single_device.enabled";

    private final TenantSingleDeviceConfigRepository tenantSingleDeviceConfigRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final StringRedisTemplate redisTemplate;

    public SingleDeviceService(
            TenantSingleDeviceConfigRepository tenantSingleDeviceConfigRepository,
            SystemConfigRepository systemConfigRepository,
            StringRedisTemplate redisTemplate) {
        this.tenantSingleDeviceConfigRepository = tenantSingleDeviceConfigRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Resolves single-device enforcement with three-layer priority:
     * 1. Account-type level config (tenantId + accountType)
     * 2. Tenant level config     (tenantId, accountType = null)
     * 3. Global system_config    (key = "single_device.enabled")
     * Falls back to false when no config exists at any level.
     */
    public boolean isEnabled(String tenantId, AccountType accountType) {
        // Layer 1: account-type level
        var accountTypeConfig = tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(tenantId, accountType);
        if (accountTypeConfig.isPresent()) {
            return accountTypeConfig.get().isEnabled();
        }

        // Layer 2: tenant level (accountType = null)
        var tenantConfig = tenantSingleDeviceConfigRepository
                .findByTenantIdAndAccountType(tenantId, null);
        if (tenantConfig.isPresent()) {
            return tenantConfig.get().isEnabled();
        }

        // Layer 3: global system config
        return systemConfigRepository.findById(GLOBAL_CONFIG_KEY)
                .map(sc -> "true".equalsIgnoreCase(sc.getConfigValue()))
                .orElse(false);
    }

    /**
     * Records a new login for the given account + device.
     * Atomically swaps the stored JTI and resets the TTL.
     *
     * @return the previous JTI if one existed (caller should blacklist it),
     *         or empty if this is the first login on this device.
     */
    public Optional<String> trackLogin(
            String accountId, String deviceType, String newJti, Duration tokenTtl) {
        String key = activeKey(accountId, deviceType);
        String oldJti = redisTemplate.opsForValue().getAndSet(key, newJti);
        redisTemplate.expire(key, tokenTtl);
        return Optional.ofNullable(oldJti);
    }

    /**
     * Returns the currently active JTI for the given account + device,
     * or empty if none is recorded.
     */
    public Optional<String> getActiveJti(String accountId, String deviceType) {
        String value = redisTemplate.opsForValue().get(activeKey(accountId, deviceType));
        return Optional.ofNullable(value);
    }

    private String activeKey(String accountId, String deviceType) {
        return ACTIVE_KEY_PREFIX + accountId + ":" + deviceType;
    }
}
