package com.caa.auth.service;

import com.caa.auth.model.Account;
import com.caa.auth.model.Permission;
import com.caa.auth.model.RolePermission;
import com.caa.auth.repository.PermissionRepository;
import com.caa.auth.repository.RolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {

    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock PermissionRepository      permissionRepository;
    @Mock StringRedisTemplate       redisTemplate;
    @Mock SetOperations<String, String> setOps;

    RolePermissionService service;

    private static final String TENANT_ID    = "ten-uuid-1";
    private static final String PERMISSION_ID = "perm-uuid-1";
    private static final String PERM_CODE    = "agent:read";
    private static final Account.AccountType ACCOUNT_TYPE = Account.AccountType.STUDENT;
    private static final String REDIS_KEY =
            "permission:role:" + TENANT_ID + ":" + ACCOUNT_TYPE.name();

    @BeforeEach
    void setUp() {
        service = new RolePermissionService(rolePermissionRepository, permissionRepository, redisTemplate);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private RolePermission buildRolePermission(boolean enabled) {
        RolePermission rp = new RolePermission();
        rp.setTenantId(TENANT_ID);
        rp.setAccountType(ACCOUNT_TYPE);
        rp.setPermissionId(PERMISSION_ID);
        rp.setEnabled(enabled);
        return rp;
    }

    private Permission buildPermission(String code) {
        Permission p = new Permission();
        p.setId(PERMISSION_ID);
        p.setCode(code);
        p.setName("Agent Read");
        p.setModule("agent");
        p.setAction("read");
        return p;
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getEnabledPermissionCodes returns from cache when cache hit")
    void getEnabledPermissionCodes_cacheHit() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(REDIS_KEY)).thenReturn(Set.of(PERM_CODE));

        Set<String> codes = service.getEnabledPermissionCodes(TENANT_ID, ACCOUNT_TYPE);

        assertThat(codes).containsExactly(PERM_CODE);
        verifyNoInteractions(rolePermissionRepository);
        verifyNoInteractions(permissionRepository);
    }

    @Test
    @DisplayName("getEnabledPermissionCodes loads from DB when cache miss")
    void getEnabledPermissionCodes_cacheMiss_loadsFromDb() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(REDIS_KEY)).thenReturn(null);

        RolePermission rp = buildRolePermission(true);
        when(rolePermissionRepository.findAllByTenantIdAndAccountType(TENANT_ID, ACCOUNT_TYPE))
                .thenReturn(List.of(rp));
        when(permissionRepository.findById(PERMISSION_ID))
                .thenReturn(java.util.Optional.of(buildPermission(PERM_CODE)));

        Set<String> codes = service.getEnabledPermissionCodes(TENANT_ID, ACCOUNT_TYPE);

        assertThat(codes).containsExactly(PERM_CODE);
        verify(setOps).add(eq(REDIS_KEY), eq(PERM_CODE));
        verify(redisTemplate).expire(eq(REDIS_KEY), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("updatePermissions saves entries and evicts cache")
    void updatePermissions_savesAndEvictsCache() {
        List<String> permissionIds = List.of(PERMISSION_ID);

        service.updatePermissions(TENANT_ID, ACCOUNT_TYPE, permissionIds);

        verify(rolePermissionRepository).deleteAll(anyList());
        verify(rolePermissionRepository).saveAll(anyList());
        verify(redisTemplate).delete(REDIS_KEY);
    }

    @Test
    @DisplayName("evictCache deletes correct Redis key")
    void evictCache_deletesCorrectKey() {
        service.evictCache(TENANT_ID, ACCOUNT_TYPE);

        verify(redisTemplate).delete(REDIS_KEY);
    }
}
