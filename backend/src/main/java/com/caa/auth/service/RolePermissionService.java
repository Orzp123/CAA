package com.caa.auth.service;

import com.caa.auth.model.Account;
import com.caa.auth.model.Permission;
import com.caa.auth.model.RolePermission;
import com.caa.auth.repository.PermissionRepository;
import com.caa.auth.repository.RolePermissionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages role-level permissions per tenant.
 *
 * <p>Redis key: {@code permission:role:{tenantId}:{accountType}} → Set of permission codes, TTL 10 min.
 */
@Service
public class RolePermissionService {

    private static final String KEY_PREFIX = "permission:role:";
    private static final long   TTL_MINUTES = 10L;

    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository     permissionRepository;
    private final StringRedisTemplate      redisTemplate;

    public RolePermissionService(RolePermissionRepository rolePermissionRepository,
                                 PermissionRepository permissionRepository,
                                 StringRedisTemplate redisTemplate) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository     = permissionRepository;
        this.redisTemplate            = redisTemplate;
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Returns all {@link RolePermission} rows for the given tenant and account type.
     */
    public List<RolePermission> findByTenantIdAndAccountType(String tenantId,
                                                              Account.AccountType accountType) {
        return rolePermissionRepository.findAllByTenantIdAndAccountType(tenantId, accountType);
    }

    /**
     * Returns the set of enabled permission codes for the given tenant + role,
     * reading from the Redis cache first and falling back to the DB on a miss.
     */
    public Set<String> getEnabledPermissionCodes(String tenantId,
                                                  Account.AccountType accountType) {
        String key = cacheKey(tenantId, accountType);

        Set<String> cached = redisTemplate.opsForSet().members(key);
        if (cached != null) {
            return cached;
        }

        // Cache miss — load from DB
        List<RolePermission> rows =
                rolePermissionRepository.findAllByTenantIdAndAccountType(tenantId, accountType);

        Set<String> codes = rows.stream()
                .filter(RolePermission::isEnabled)
                .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getCode())
                .collect(Collectors.toSet());

        if (!codes.isEmpty()) {
            redisTemplate.opsForSet().add(key, codes.toArray(new String[0]));
            redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
        }

        return codes;
    }

    /**
     * Replaces all role-permission assignments for the given tenant + role,
     * then evicts the cache.
     */
    @Transactional
    public void updatePermissions(String tenantId,
                                  Account.AccountType accountType,
                                  List<String> permissionIds) {
        List<RolePermission> existing =
                rolePermissionRepository.findAllByTenantIdAndAccountType(tenantId, accountType);
        rolePermissionRepository.deleteAll(existing);

        List<RolePermission> replacements = permissionIds.stream()
                .map(pid -> {
                    RolePermission rp = new RolePermission();
                    rp.setTenantId(tenantId);
                    rp.setAccountType(accountType);
                    rp.setPermissionId(pid);
                    rp.setEnabled(true);
                    return rp;
                })
                .collect(Collectors.toList());

        rolePermissionRepository.saveAll(replacements);
        evictCache(tenantId, accountType);
    }

    /**
     * Evicts the Redis cache entry for the given tenant + role.
     */
    public void evictCache(String tenantId, Account.AccountType accountType) {
        redisTemplate.delete(cacheKey(tenantId, accountType));
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    private String cacheKey(String tenantId, Account.AccountType accountType) {
        return KEY_PREFIX + tenantId + ":" + accountType.name();
    }
}
