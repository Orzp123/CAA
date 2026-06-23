package com.caa.auth.repository;

import com.caa.auth.model.Account;
import com.caa.auth.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {

    List<RolePermission> findAllByTenantIdAndAccountType(String tenantId,
                                                         Account.AccountType accountType);

    Optional<RolePermission> findByTenantIdAndAccountTypeAndPermissionId(String tenantId,
                                                                          Account.AccountType accountType,
                                                                          String permissionId);
}
