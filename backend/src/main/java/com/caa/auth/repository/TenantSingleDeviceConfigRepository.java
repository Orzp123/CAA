package com.caa.auth.repository;

import com.caa.auth.model.Account.AccountType;
import com.caa.auth.model.TenantSingleDeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantSingleDeviceConfigRepository extends JpaRepository<TenantSingleDeviceConfig, String> {

    Optional<TenantSingleDeviceConfig> findByTenantIdAndAccountType(String tenantId, AccountType accountType);

    List<TenantSingleDeviceConfig> findAllByTenantId(String tenantId);

    Optional<TenantSingleDeviceConfig> findByTenantIdIsNullAndAccountTypeIsNull();
}
