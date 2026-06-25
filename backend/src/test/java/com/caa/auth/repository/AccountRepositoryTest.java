package com.caa.auth.repository;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired AccountRepository accountRepository;
    @Autowired TenantRepository tenantRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setCode("pku");
        tenant.setName("北京大学");
        tenant.setType(Tenant.TenantType.SCHOOL);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        tenantRepository.save(tenant);
    }

    private Account buildAccount(String studentNo) {
        Account a = new Account();
        a.setTenantId(tenant.getId());
        a.setStudentNo(studentNo);
        a.setName("测试用户");
        a.setAccountType(Account.AccountType.STUDENT);
        a.setStatus(Account.AccountStatus.ACTIVE);
        return a;
    }

    @Test
    @DisplayName("findByTenantIdAndStudentNo returns account when exists")
    void findByTenantIdAndStudentNo_found() {
        accountRepository.save(buildAccount("2024001"));
        Optional<Account> result = accountRepository.findByTenantIdAndStudentNo(tenant.getId(), "2024001");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("测试用户");
    }

    @Test
    @DisplayName("findByTenantIdAndStudentNo returns empty when missing")
    void findByTenantIdAndStudentNo_notFound() {
        assertThat(accountRepository.findByTenantIdAndStudentNo(tenant.getId(), "9999")).isEmpty();
    }

    @Test
    @DisplayName("findByWechatOpenid returns account when openid matches")
    void findByWechatOpenid_found() {
        Account a = buildAccount("2024002");
        a.setWechatOpenid("wx_openid_abc");
        accountRepository.save(a);
        Optional<Account> result = accountRepository.findByWechatOpenid("wx_openid_abc");
        assertThat(result).isPresent();
        assertThat(result.get().getStudentNo()).isEqualTo("2024002");
    }

    @Test
    @DisplayName("findByTenantIdAndSsoSubject returns account when subject matches")
    void findByTenantIdAndSsoSubject_found() {
        Account a = buildAccount("2024003");
        a.setSsoSubject("idp_subject_xyz");
        accountRepository.save(a);
        Optional<Account> result = accountRepository.findByTenantIdAndSsoSubject(tenant.getId(), "idp_subject_xyz");
        assertThat(result).isPresent();
        assertThat(result.get().getStudentNo()).isEqualTo("2024003");
    }

    @Test
    @DisplayName("findAllByTenantIdAndStatus returns locked accounts only")
    void findAllByTenantIdAndStatus() {
        accountRepository.save(buildAccount("2024004"));
        Account locked = buildAccount("2024005");
        locked.setStatus(Account.AccountStatus.LOCKED);
        accountRepository.save(locked);
        var lockedList = accountRepository.findAllByTenantIdAndStatus(tenant.getId(), Account.AccountStatus.LOCKED);
        assertThat(lockedList).hasSize(1);
        assertThat(lockedList.get(0).getStudentNo()).isEqualTo("2024005");
    }
}
