package com.caa.auth.repository;

import com.caa.auth.model.Account;
import com.caa.auth.model.Permission;
import com.caa.auth.model.RolePermission;
import com.caa.auth.model.Tenant;
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
class RolePermissionRepositoryTest {

    @Autowired RolePermissionRepository rolePermissionRepository;
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

    private RolePermission buildRolePermission(Account.AccountType accountType,
                                               String permId, boolean enabled) {
        RolePermission rp = new RolePermission();
        rp.setTenantId(tenant.getId());
        rp.setAccountType(accountType);
        rp.setPermissionId(permId);
        rp.setEnabled(enabled);
        return rp;
    }

    @Test
    @DisplayName("findAllByTenantIdAndAccountType returns records for the given role")
    void findAllByTenantIdAndAccountType_found() {
        rolePermissionRepository.save(
                buildRolePermission(Account.AccountType.TEACHER, permission.getId(), true));

        Permission p2 = new Permission();
        p2.setCode("agent:read");
        p2.setName("agent:read");
        p2.setModule("agent");
        p2.setAction("read");
        p2.setStatus(Permission.PermissionStatus.ACTIVE);
        p2.setSystem(false);
        permissionRepository.save(p2);

        rolePermissionRepository.save(
                buildRolePermission(Account.AccountType.STUDENT, p2.getId(), true));

        List<RolePermission> result = rolePermissionRepository
                .findAllByTenantIdAndAccountType(tenant.getId(), Account.AccountType.TEACHER);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPermissionId()).isEqualTo(permission.getId());
    }

    @Test
    @DisplayName("findAllByTenantIdAndAccountType returns empty when no records for role")
    void findAllByTenantIdAndAccountType_empty() {
        assertThat(rolePermissionRepository
                .findAllByTenantIdAndAccountType(tenant.getId(), Account.AccountType.SCHOOL_ADMIN))
                .isEmpty();
    }

    @Test
    @DisplayName("findByTenantIdAndAccountTypeAndPermissionId returns record when all match")
    void findByTenantIdAndAccountTypeAndPermissionId_found() {
        rolePermissionRepository.save(
                buildRolePermission(Account.AccountType.TEACHER, permission.getId(), true));

        Optional<RolePermission> result = rolePermissionRepository
                .findByTenantIdAndAccountTypeAndPermissionId(
                        tenant.getId(), Account.AccountType.TEACHER, permission.getId());
        assertThat(result).isPresent();
        assertThat(result.get().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("findByTenantIdAndAccountTypeAndPermissionId returns empty when role mismatch")
    void findByTenantIdAndAccountTypeAndPermissionId_roleMismatch() {
        rolePermissionRepository.save(
                buildRolePermission(Account.AccountType.TEACHER, permission.getId(), true));

        Optional<RolePermission> result = rolePermissionRepository
                .findByTenantIdAndAccountTypeAndPermissionId(
                        tenant.getId(), Account.AccountType.STUDENT, permission.getId());
        assertThat(result).isEmpty();
    }
}
