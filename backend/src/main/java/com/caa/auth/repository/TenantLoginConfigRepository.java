package com.caa.auth.repository;

import com.caa.auth.model.Tenant;
import com.caa.auth.model.TenantLoginConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantLoginConfigRepository extends JpaRepository<TenantLoginConfig, String> {

    List<TenantLoginConfig> findAllByTenantId(String tenantId);

    Optional<TenantLoginConfig> findByTenantIdAndLoginType(String tenantId, Tenant.LoginType loginType);

    Optional<TenantLoginConfig> findByTenantIdAndIsDefaultTrue(String tenantId);
}
