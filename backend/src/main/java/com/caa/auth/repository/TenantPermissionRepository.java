package com.caa.auth.repository;

import com.caa.auth.model.TenantPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantPermissionRepository extends JpaRepository<TenantPermission, String> {

    List<TenantPermission> findAllByTenantIdAndEnabled(String tenantId, boolean enabled);

    Optional<TenantPermission> findByTenantIdAndPermissionId(String tenantId, String permissionId);
}
