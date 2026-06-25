package com.caa.auth.integration;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tenant isolation tests using @DataJpaTest (Spring Boot 4.x — org.springframework.boot.data.jpa.test.autoconfigure).
 *
 * <p>Verifies:
 * <ol>
 *   <li>An account in tenant A cannot be found when queried under tenant B.</li>
 *   <li>Creating a duplicate studentNo in the same tenant raises a constraint violation.</li>
 *   <li>Same studentNo in different tenants is allowed.</li>
 * </ol>
 */
@DataJpaTest
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AccountRepository accountRepository;

    private String tenantAId;
    private String tenantBId;

    @BeforeEach
    void setUp() {
        Tenant tenantA = new Tenant();
        tenantA.setCode("school-a");
        tenantA.setName("School A");
        tenantA.setType(Tenant.TenantType.SCHOOL);
        tenantA.setStatus(Tenant.TenantStatus.ACTIVE);
        tenantA.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        tenantAId = tenantRepository.save(tenantA).getId();

        Tenant tenantB = new Tenant();
        tenantB.setCode("school-b");
        tenantB.setName("School B");
        tenantB.setType(Tenant.TenantType.SCHOOL);
        tenantB.setStatus(Tenant.TenantStatus.ACTIVE);
        tenantB.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        tenantBId = tenantRepository.save(tenantB).getId();
    }

    // ── Test 1: Account in tenant A not visible from tenant B ────────────────

    @Test
    void account_inTenantA_notFoundUnderTenantB() {
        Account account = new Account();
        account.setTenantId(tenantAId);
        account.setStudentNo("S001");
        account.setName("Alice");
        account.setAccountType(Account.AccountType.STUDENT);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setPasswordHash("$2a$10$placeholder");
        accountRepository.save(account);

        Optional<Account> foundInA = accountRepository
                .findByTenantIdAndStudentNo(tenantAId, "S001");
        assertThat(foundInA).isPresent();
        assertThat(foundInA.get().getTenantId()).isEqualTo(tenantAId);

        Optional<Account> foundInB = accountRepository
                .findByTenantIdAndStudentNo(tenantBId, "S001");
        assertThat(foundInB).isEmpty();
    }

    // ── Test 2: Duplicate studentNo in same tenant is rejected ───────────────

    @Test
    void duplicateStudentNo_inSameTenant_violatesConstraint() {
        Account first = new Account();
        first.setTenantId(tenantAId);
        first.setStudentNo("S002");
        first.setName("Bob");
        first.setAccountType(Account.AccountType.STUDENT);
        first.setStatus(Account.AccountStatus.ACTIVE);
        first.setPasswordHash("$2a$10$placeholder");
        accountRepository.saveAndFlush(first);

        Account duplicate = new Account();
        duplicate.setTenantId(tenantAId);
        duplicate.setStudentNo("S002");
        duplicate.setName("Bob Clone");
        duplicate.setAccountType(Account.AccountType.STUDENT);
        duplicate.setStatus(Account.AccountStatus.ACTIVE);
        duplicate.setPasswordHash("$2a$10$placeholder");

        assertThatThrownBy(() -> accountRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Test 3: Same studentNo in different tenants is allowed ───────────────

    @Test
    void sameStudentNo_inDifferentTenants_isAllowed() {
        Account inA = new Account();
        inA.setTenantId(tenantAId);
        inA.setStudentNo("S003");
        inA.setName("Charlie in A");
        inA.setAccountType(Account.AccountType.STUDENT);
        inA.setStatus(Account.AccountStatus.ACTIVE);
        inA.setPasswordHash("$2a$10$placeholder");
        accountRepository.saveAndFlush(inA);

        Account inB = new Account();
        inB.setTenantId(tenantBId);
        inB.setStudentNo("S003");
        inB.setName("Charlie in B");
        inB.setAccountType(Account.AccountType.STUDENT);
        inB.setStatus(Account.AccountStatus.ACTIVE);
        inB.setPasswordHash("$2a$10$placeholder");
        accountRepository.saveAndFlush(inB);

        assertThat(accountRepository.findByTenantIdAndStudentNo(tenantAId, "S003")).isPresent();
        assertThat(accountRepository.findByTenantIdAndStudentNo(tenantBId, "S003")).isPresent();
    }
}
