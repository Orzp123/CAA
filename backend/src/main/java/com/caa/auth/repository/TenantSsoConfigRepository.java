package com.caa.auth.repository;

import com.caa.auth.model.TenantSsoConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantSsoConfigRepository extends JpaRepository<TenantSsoConfig, String> {

    Optional<TenantSsoConfig> findByTenantId(String tenantId);

    Optional<TenantSsoConfig> findByTenantIdAndEnabled(String tenantId, boolean enabled);
}
