package com.caa.auth.repository;

import com.caa.auth.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByTenantIdAndStudentNo(String tenantId, String studentNo);

    Optional<Account> findByWechatOpenid(String wechatOpenid);

    Optional<Account> findByTenantIdAndSsoSubject(String tenantId, String ssoSubject);

    List<Account> findAllByTenantIdAndStatus(String tenantId, Account.AccountStatus status);

    List<Account> findAllByTenantId(String tenantId);
}
