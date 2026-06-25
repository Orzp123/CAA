package com.caa.auth.service;

import com.caa.auth.model.Permission;
import com.caa.auth.model.TenantPermission;
import com.caa.auth.repository.PermissionRepository;
import com.caa.auth.repository.TenantPermissionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TenantPermissionService {

    private static final String CACHE_KEY_PREFIX = "permission:tenant:";
    private static final long CACHE_TTL_MINUTES = 10L;

    private final TenantPermissionRepository tenantPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public TenantPermissionService(TenantPermissionRepository tenantPermissionRepository,
                                   PermissionRepository permissionRepository,
                                   StringRedisTemplate stringRedisTemplate) {
        this.tenantPermissionRepository = tenantPermissionRepository;
        this.permissionRepository = permissionRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public List<TenantPermission> findByTenantId(String tenantId) {
        return tenantPermissionRepository.findAllByTenantIdAndEnabled(tenantId, true);
    }

    public Set<String> getEnabledPermissionCodes(String tenantId) {
        String key = cacheKey(tenantId);
        Set<String> cached = stringRedisTemplate.opsForSet().members(key);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // Cache miss — load from DB
        List<TenantPermission> tenantPermissions =
                tenantPermissionRepository.findAllByTenantIdAndEnabled(tenantId, true);

        Set<String> codes = new HashSet<>();
        for (TenantPermission tp : tenantPermissions) {
            Optional<Permission> permission = permissionRepository.findById(tp.getPermissionId());
            permission.map(Permission::getCode).ifPresent(codes::add);
        }

        if (!codes.isEmpty()) {
            stringRedisTemplate.opsForSet().add(key, codes.toArray(new String[0]));
            stringRedisTemplate.expire(key, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return codes;
    }

    @Transactional
    public void updatePermissions(String tenantId, List<String> permissionIds) {
        // Delete existing enabled records
        List<TenantPermission> existing =
                tenantPermissionRepository.findAllByTenantIdAndEnabled(tenantId, true);
        tenantPermissionRepository.deleteAll(existing);

        // Save new records
        for (String permissionId : permissionIds) {
            TenantPermission tp = new TenantPermission();
            tp.setTenantId(tenantId);
            tp.setPermissionId(permissionId);
            tp.setEnabled(true);
            tenantPermissionRepository.save(tp);
        }

        // Evict cache
        evictCache(tenantId);
    }

    public void evictCache(String tenantId) {
        stringRedisTemplate.delete(cacheKey(tenantId));
    }

    private String cacheKey(String tenantId) {
        return CACHE_KEY_PREFIX + tenantId;
    }
}
