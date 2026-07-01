package com.caa.auth.repository;

import com.caa.auth.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String>,
        JpaSpecificationExecutor<Account> {

    Optional<Account> findByTenantIdAndStudentNo(String tenantId, String studentNo);

    Optional<Account> findByWechatOpenid(String wechatOpenid);

    Optional<Account> findByTenantIdAndSsoSubject(String tenantId, String ssoSubject);

    List<Account> findAllByTenantIdAndStatus(String tenantId, Account.AccountStatus status);

    List<Account> findAllByTenantId(String tenantId);

    long countByTenantIdAndAccountType(String tenantId, Account.AccountType accountType);

    List<Account> findAllByIdInAndTenantId(List<String> ids, String tenantId);

    // H-5 fix: 一次查询同时校验 tenantId，避免先查后校验的 TOCTOU 问题
    java.util.Optional<Account> findByIdAndTenantId(String id, String tenantId);
}
