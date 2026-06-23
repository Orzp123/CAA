package com.caa.auth.service;

import com.caa.auth.model.Permission;
import com.caa.auth.model.TenantPermission;
import com.caa.auth.repository.PermissionRepository;
import com.caa.auth.repository.TenantPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantPermissionServiceTest {

    @Mock
    private TenantPermissionRepository tenantPermissionRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private TenantPermissionService tenantPermissionService;

    private static final String TENANT_ID = "tenant-1";
    private static final String REDIS_KEY = "permission:tenant:tenant-1";

    private TenantPermission tenantPermission;
    private Permission permission;

    @BeforeEach
    void setUp() {
        permission = new Permission();
        permission.setId("perm-1");
        permission.setCode("USER_READ");
        permission.setName("Read User");
        permission.setModule("user");
        permission.setAction("read");
        permission.setStatus(Permission.PermissionStatus.ACTIVE);

        tenantPermission = new TenantPermission();
        tenantPermission.setId("tp-1");
        tenantPermission.setTenantId(TENANT_ID);
        tenantPermission.setPermissionId("perm-1");
        tenantPermission.setEnabled(true);
    }

    @Test
    void getEnabledPermissionCodes_returnsFromCache_whenCacheHit() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(REDIS_KEY)).thenReturn(Set.of("USER_READ"));

        Set<String> result = tenantPermissionService.getEnabledPermissionCodes(TENANT_ID);

        assertThat(result).containsExactly("USER_READ");
        // DB should NOT be called on cache hit
        verifyNoInteractions(tenantPermissionRepository);
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void getEnabledPermissionCodes_loadsFromDbAndCaches_whenCacheMiss() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(REDIS_KEY)).thenReturn(null);
        when(tenantPermissionRepository.findAllByTenantIdAndEnabled(TENANT_ID, true))
                .thenReturn(List.of(tenantPermission));
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(permission));

        Set<String> result = tenantPermissionService.getEnabledPermissionCodes(TENANT_ID);

        assertThat(result).containsExactly("USER_READ");
        // Should cache the result
        verify(setOperations).add(eq(REDIS_KEY), eq("USER_READ"));
        verify(stringRedisTemplate).expire(eq(REDIS_KEY), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void updatePermissions_savesNewListAndEvictsCache() {
        Permission perm2 = new Permission();
        perm2.setId("perm-2");
        perm2.setCode("USER_WRITE");

        when(tenantPermissionRepository.findAllByTenantIdAndEnabled(TENANT_ID, true))
                .thenReturn(List.of(tenantPermission));
        when(permissionRepository.findById("perm-2")).thenReturn(Optional.of(perm2));
        when(tenantPermissionRepository.save(any(TenantPermission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        tenantPermissionService.updatePermissions(TENANT_ID, List.of("perm-2"));

        // Old records should be deleted
        verify(tenantPermissionRepository).deleteAll(List.of(tenantPermission));
        // New record should be saved
        verify(tenantPermissionRepository).save(any(TenantPermission.class));
        // Cache must be evicted
        verify(stringRedisTemplate).delete(REDIS_KEY);
    }

    @Test
    void evictCache_callsRedisDeleteWithCorrectKey() {
        tenantPermissionService.evictCache(TENANT_ID);

        verify(stringRedisTemplate).delete(REDIS_KEY);
    }
}
