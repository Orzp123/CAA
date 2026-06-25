package com.caa.auth.repository;

import com.caa.auth.model.Permission;
import com.caa.auth.model.Tenant;
import com.caa.auth.model.TenantPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TenantPermissionRepositoryTest {

    @Autowired TenantPermissionRepository tenantPermissionRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired PermissionRepository permissionRepository;

    private Tenant tenant;
    private Permission permission;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setCode("pku");
        tenant.setName("北京大学");
        tenant.setType(Tenant.TenantType.SCHOOL);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        tenantRepository.save(tenant);

        permission = new Permission();
        permission.setCode("agent:create");
        permission.setName("agent:create");
        permission.setModule("agent");
        permission.setAction("create");
        permission.setStatus(Permission.PermissionStatus.ACTIVE);
        permission.setSystem(false);
        permissionRepository.save(permission);
    }

    private TenantPermission buildTenantPermission(boolean enabled) {
        TenantPermission tp = new TenantPermission();
        tp.setTenantId(tenant.getId());
        tp.setPermissionId(permission.getId());
        tp.setEnabled(enabled);
        return tp;
    }

    @Test
    @DisplayName("findAllByTenantIdAndEnabled returns enabled records for tenant")
    void findAllByTenantIdAndEnabled_found() {
        tenantPermissionRepository.save(buildTenantPermission(true));

        Permission p2 = new Permission();
        p2.setCode("agent:delete");
        p2.setName("agent:delete");
        p2.setModule("agent");
        p2.setAction("delete");
        p2.setStatus(Permission.PermissionStatus.ACTIVE);
        p2.setSystem(false);
        permissionRepository.save(p2);

        TenantPermission disabled = new TenantPermission();
        disabled.setTenantId(tenant.getId());
        disabled.setPermissionId(p2.getId());
        disabled.setEnabled(false);
        tenantPermissionRepository.save(disabled);

        List<TenantPermission> result = tenantPermissionRepository
                .findAllByTenantIdAndEnabled(tenant.getId(), true);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPermissionId()).isEqualTo(permission.getId());
    }

    @Test
    @DisplayName("findByTenantIdAndPermissionId returns record when both match")
    void findByTenantIdAndPermissionId_found() {
        tenantPermissionRepository.save(buildTenantPermission(true));
        Optional<TenantPermission> result = tenantPermissionRepository
                .findByTenantIdAndPermissionId(tenant.getId(), permission.getId());
        assertThat(result).isPresent();
        assertThat(result.get().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("findByTenantIdAndPermissionId returns empty when not assigned")
    void findByTenantIdAndPermissionId_notFound() {
        assertThat(tenantPermissionRepository
                .findByTenantIdAndPermissionId(tenant.getId(), "no-such-id")).isEmpty();
    }
}
