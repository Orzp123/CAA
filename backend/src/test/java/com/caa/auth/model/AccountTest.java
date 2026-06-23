package com.caa.auth.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    void defaultStatus_isActive() {
        assertThat(new Account().getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
    }

    @Test
    void defaultLoginFailCount_isZero() {
        assertThat(new Account().getLoginFailCount()).isZero();
    }

    @Test
    void prePersist_setsIdAndTimestamps() {
        Account account = buildAccount();
        account.prePersist();

        assertThat(account.getId()).isNotNull();
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
    }

    @Test
    void prePersist_doesNotOverwriteExistingId() {
        Account account = buildAccount();
        account.setId("fixed-id");
        account.prePersist();

        assertThat(account.getId()).isEqualTo("fixed-id");
    }

    @Test
    void preUpdate_refreshesUpdatedAt() throws InterruptedException {
        Account account = buildAccount();
        account.prePersist();
        var before = account.getUpdatedAt();

        Thread.sleep(2);
        account.preUpdate();

        assertThat(account.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void settersAndGetters_roundTrip() {
        Account account = new Account();
        account.setTenantId("tenant-1");
        account.setStudentNo("20240001");
        account.setName("张三");
        account.setNickname("小张");
        account.setAccountType(Account.AccountType.STUDENT);
        account.setPasswordHash("$2a$10$hash");
        account.setWechatOpenid("openid-abc");
        account.setWechatUnionid("unionid-xyz");
        account.setSsoSubject("sub-001");
        account.setLoginFailCount(3);
        account.setStatus(Account.AccountStatus.LOCKED);
        LocalDateTime lockTime = LocalDateTime.now().plusMinutes(15);
        account.setLockedUntil(lockTime);

        assertThat(account.getTenantId()).isEqualTo("tenant-1");
        assertThat(account.getStudentNo()).isEqualTo("20240001");
        assertThat(account.getName()).isEqualTo("张三");
        assertThat(account.getNickname()).isEqualTo("小张");
        assertThat(account.getAccountType()).isEqualTo(Account.AccountType.STUDENT);
        assertThat(account.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(account.getWechatOpenid()).isEqualTo("openid-abc");
        assertThat(account.getWechatUnionid()).isEqualTo("unionid-xyz");
        assertThat(account.getSsoSubject()).isEqualTo("sub-001");
        assertThat(account.getLoginFailCount()).isEqualTo(3);
        assertThat(account.getStatus()).isEqualTo(Account.AccountStatus.LOCKED);
        assertThat(account.getLockedUntil()).isEqualTo(lockTime);
    }

    @Test
    void allAccountTypeValues_exist() {
        assertThat(Account.AccountType.values())
                .containsExactlyInAnyOrder(
                        Account.AccountType.SYSTEM_ADMIN,
                        Account.AccountType.SCHOOL_ADMIN,
                        Account.AccountType.TEACHER,
                        Account.AccountType.STUDENT);
    }

    @Test
    void allAccountStatusValues_exist() {
        assertThat(Account.AccountStatus.values())
                .containsExactlyInAnyOrder(
                        Account.AccountStatus.ACTIVE,
                        Account.AccountStatus.DISABLED,
                        Account.AccountStatus.LOCKED);
    }

    @Test
    void lockedUntil_isNullByDefault() {
        assertThat(new Account().getLockedUntil()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Account buildAccount() {
        Account a = new Account();
        a.setTenantId("tenant-1");
        a.setStudentNo("20240001");
        a.setName("测试账户");
        a.setAccountType(Account.AccountType.STUDENT);
        return a;
    }
}
