package com.caa.auth.service;

import com.caa.auth.exception.ResourceNotFoundException;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock TenantRepository    tenantRepository;
    @Mock StringRedisTemplate redisTemplate;

    TenantService service;

    private static final String TENANT_ID = "ten-uuid-1";
    private static final String CODE      = "pku";
    private static final String NAME      = "北京大学";
    private static final String DOMAIN    = "pku.edu.cn";

    @BeforeEach
    void setUp() {
        service = new TenantService(tenantRepository, redisTemplate);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Tenant buildTenant(Tenant.TenantType type, Tenant.TenantStatus status) {
        Tenant t = new Tenant();
        t.setId(TENANT_ID);
        t.setCode(CODE);
        t.setName(NAME);
        t.setType(type);
        t.setStatus(status);
        t.setDomain(DOMAIN);
        t.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        return t;
    }

    // ── findByCode ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByCode returns tenant when found")
    void findByCode_found() {
        when(tenantRepository.findByCode(CODE))
                .thenReturn(Optional.of(buildTenant(Tenant.TenantType.SCHOOL, Tenant.TenantStatus.ACTIVE)));

        Tenant result = service.findByCode(CODE);

        assertThat(result.getCode()).isEqualTo(CODE);
        assertThat(result.getName()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("findByCode throws ResourceNotFoundException when not found")
    void findByCode_notFound_throws() {
        when(tenantRepository.findByCode(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode(CODE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(CODE);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create saves new tenant with ACTIVE status")
    void create_savesWithActiveStatus() {
        when(tenantRepository.save(any(Tenant.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Tenant result = service.create(CODE, NAME, Tenant.TenantType.SCHOOL, Tenant.LoginType.PASSWORD);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());

        Tenant saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo(CODE);
        assertThat(saved.getName()).isEqualTo(NAME);
        assertThat(saved.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
        assertThat(result.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus to INACTIVE evicts Redis cache keys")
    void updateStatus_inactive_evictsCache() {
        Tenant tenant = buildTenant(Tenant.TenantType.SCHOOL, Tenant.TenantStatus.ACTIVE);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(TENANT_ID, Tenant.TenantStatus.INACTIVE);

        verify(redisTemplate).delete("tenant:info:" + TENANT_ID);
        verify(redisTemplate).delete("tenant:domain:" + DOMAIN);
    }

    @Test
    @DisplayName("updateStatus throws IllegalArgumentException when deactivating ADMIN tenant")
    void updateStatus_adminTenant_throws() {
        Tenant adminTenant = buildTenant(Tenant.TenantType.ADMIN, Tenant.TenantStatus.ACTIVE);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(adminTenant));

        assertThatThrownBy(() -> service.updateStatus(TENANT_ID, Tenant.TenantStatus.INACTIVE))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tenantRepository, never()).save(any());
    }
}
