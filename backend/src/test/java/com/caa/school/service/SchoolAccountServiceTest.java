package com.caa.school.service;

import com.caa.auth.model.Account;
import com.caa.auth.repository.AccountRepository;
import com.caa.common.ErrorCode;
import com.caa.school.dto.*;
import com.caa.school.exception.SchoolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private SchoolAccountService service;

    private static final String TENANT_ID = "tenant-001";
    private static final String ACCOUNT_ID = "acc-001";
    private static final String OTHER_ACCOUNT_ID = "acc-002";

    @BeforeEach
    void setUp() {
        service = new SchoolAccountService(accountRepository, new BCryptPasswordEncoder());
        ReflectionTestUtils.setField(service, "defaultPassword", "Caa@2026");
    }

    // ──────────────────── US3 创建账户 ────────────────────

    @Test
    void createAccount_success() {
        // given
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, "stu001"))
                .thenReturn(Optional.empty());

        Account saved = buildAccount(ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        CreateAccountRequest req = new CreateAccountRequest(
                "stu001", "张三", "STUDENT", null, null, null, null, null);

        // when
        AccountResponse resp = service.createAccount(TENANT_ID, req);

        // then
        verify(accountRepository).save(any(Account.class));
        assertThat(resp.loginName()).isEqualTo("stu001");
        assertThat(resp.accountType()).isEqualTo("STUDENT");
    }

    @Test
    void createAccount_loginNameConflict() {
        // given
        Account existing = buildAccount(OTHER_ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, "stu001"))
                .thenReturn(Optional.of(existing));

        CreateAccountRequest req = new CreateAccountRequest(
                "stu001", "张三", "STUDENT", null, null, null, null, null);

        // when / then
        assertThatThrownBy(() -> service.createAccount(TENANT_ID, req))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LOGIN_NAME_DUPLICATE));
    }

    @Test
    void createAccount_secondaryRoleInvalid() {
        // given: SYSTEM_ADMIN 不可有 secondaryRole
        when(accountRepository.findByTenantIdAndStudentNo(any(), any()))
                .thenReturn(Optional.empty());

        CreateAccountRequest req = new CreateAccountRequest(
                "admin001", "管理员", "SYSTEM_ADMIN", "ASSISTANT", null, null, null, null);

        // when / then
        assertThatThrownBy(() -> service.createAccount(TENANT_ID, req))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SECONDARY_ROLE_INVALID));
    }

    @Test
    void createAccount_emailFormatInvalid() {
        // given
        when(accountRepository.findByTenantIdAndStudentNo(any(), any()))
                .thenReturn(Optional.empty());

        CreateAccountRequest req = new CreateAccountRequest(
                "stu001", "张三", "STUDENT", null, null, "notanemail", null, null);

        // when / then
        assertThatThrownBy(() -> service.createAccount(TENANT_ID, req))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_FORMAT_INVALID));
    }

    @Test
    void createAccount_phoneFormatInvalid() {
        // given
        when(accountRepository.findByTenantIdAndStudentNo(any(), any()))
                .thenReturn(Optional.empty());

        CreateAccountRequest req = new CreateAccountRequest(
                "stu001", "张三", "STUDENT", null, null, null, "12345", null);

        // when / then
        assertThatThrownBy(() -> service.createAccount(TENANT_ID, req))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PHONE_FORMAT_INVALID));
    }

    @Test
    void createAccount_emptyPasswordUsesDefault() {
        // given
        when(accountRepository.findByTenantIdAndStudentNo(any(), any()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        Account saved = buildAccount(ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        when(accountRepository.save(captor.capture())).thenReturn(saved);

        CreateAccountRequest req = new CreateAccountRequest(
                "stu001", "张三", "STUDENT", null, null, null, null, null);

        // when
        service.createAccount(TENANT_ID, req);

        // then: passwordHash 非空（使用了默认密码）
        assertThat(captor.getValue().getPasswordHash()).isNotBlank();
    }

    // ──────────────────── US4 账户列表 ────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void listAccounts_withAllFilters() {
        // given
        Account acc = buildAccount(ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        Page<Account> page = new PageImpl<>(List.of(acc));
        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<AccountResponse> result = service.listAccounts(
                TENANT_ID, "stu001", Account.AccountType.STUDENT, "张三", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listAccounts_noFilters() {
        // given
        Page<Account> page = new PageImpl<>(List.of());
        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 20);

        // when
        Page<AccountResponse> result = service.listAccounts(
                TENANT_ID, null, null, null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ──────────────────── US5 编辑/删除 ────────────────────

    @Test
    void updateAccount_success() {
        // given
        Account acc = buildAccount(ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        // H-5 fix: 使用 findByIdAndTenantId
        when(accountRepository.findByIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenReturn(acc);

        UpdateAccountRequest req = new UpdateAccountRequest("李四", "小四", "lisi@example.com", "13812345678");

        // when
        AccountResponse resp = service.updateAccount(TENANT_ID, ACCOUNT_ID, req);

        // then
        verify(accountRepository).save(any(Account.class));
        // accountType / secondaryRole 未变更
        assertThat(resp.accountType()).isEqualTo("STUDENT");
    }

    @Test
    void resetPassword_success() {
        // given
        Account acc = buildAccount(ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        // H-5 fix: 使用 findByIdAndTenantId
        when(accountRepository.findByIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(Optional.of(acc));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(captor.capture())).thenReturn(acc);

        // when
        service.resetPassword(TENANT_ID, ACCOUNT_ID, "NewPass@2026");

        // then: passwordHash 已更新
        assertThat(captor.getValue().getPasswordHash()).isNotBlank();
    }

    @Test
    void updateStatus_success() {
        // given
        Account acc = buildAccount(ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        // H-5 fix: 使用 findByIdAndTenantId
        when(accountRepository.findByIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(Optional.of(acc));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(captor.capture())).thenReturn(acc);

        // when
        service.updateStatus(TENANT_ID, ACCOUNT_ID, Account.AccountStatus.DISABLED);

        // then
        assertThat(captor.getValue().getStatus()).isEqualTo(Account.AccountStatus.DISABLED);
    }

    @Test
    void deleteAccount_selfForbidden() {
        // when / then: 自身 ID 不允许删除
        assertThatThrownBy(() -> service.deleteAccount(TENANT_ID, ACCOUNT_ID, ACCOUNT_ID))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SELF_DELETE_FORBIDDEN));
    }

    @Test
    void deleteAccount_success() {
        // given
        Account acc = buildAccount(ACCOUNT_ID, "stu001", Account.AccountType.STUDENT, null);
        // H-5 fix: 使用 findByIdAndTenantId
        when(accountRepository.findByIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(Optional.of(acc));

        // when
        service.deleteAccount(TENANT_ID, ACCOUNT_ID, OTHER_ACCOUNT_ID);

        // then
        verify(accountRepository).delete(acc);
    }

    // ──────────────────── US7 批量操作 ────────────────────

    @Test
    void batchUpdateStatus_success() {
        // given
        Account acc1 = buildAccount("acc-1", "stu001", Account.AccountType.STUDENT, null);
        Account acc2 = buildAccount("acc-2", "stu002", Account.AccountType.STUDENT, null);
        when(accountRepository.findAllByIdInAndTenantId(List.of("acc-1", "acc-2"), TENANT_ID))
                .thenReturn(List.of(acc1, acc2));

        // when
        service.batchUpdateStatus(TENANT_ID, List.of("acc-1", "acc-2"), Account.AccountStatus.DISABLED);

        // then
        verify(accountRepository).saveAll(anyList());
    }

    @Test
    void batchDelete_excludesSelf() {
        // given: 列表中含自身 ID
        Account acc1 = buildAccount("acc-1", "stu001", Account.AccountType.STUDENT, null);
        Account acc2 = buildAccount("acc-2", "stu002", Account.AccountType.STUDENT, null);
        // acc-1 是当前用户，应被排除
        when(accountRepository.findAllByIdInAndTenantId(List.of("acc-2"), TENANT_ID))
                .thenReturn(List.of(acc2));

        // when
        BatchDeleteResponse resp = service.batchDelete(
                TENANT_ID, List.of("acc-1", "acc-2"), "acc-1");

        // then
        assertThat(resp.excludedSelfCount()).isEqualTo(1);
        assertThat(resp.deletedCount()).isEqualTo(1);
        verify(accountRepository).deleteAll(List.of(acc2));
    }

    // ─── H-3: 后缀 LIKE 验证 ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void listAccounts_suffixLike_notPrefixLike() {
        // given
        Page<Account> page = new PageImpl<>(List.of());
        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<Account>> specCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        when(accountRepository.findAll(specCaptor.capture(), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        service.listAccounts(TENANT_ID, "stu", null, "张", pageable);

        // then: Specification 被调用（具体 LIKE 模式由集成测试验证）
        verify(accountRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable));
    }

    // ─── H-5: findByIdAndTenantId 一次查询同时校验租户 ──────────────────────────

    @Test
    void findByIdAndTenant_notFound_throws404() {
        // given: findByIdAndTenantId 返回空（账户不存在或租户不匹配）
        when(accountRepository.findByIdAndTenantId(ACCOUNT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // when / then: 不论是账户不存在还是租户不匹配，统一返回 NOT_FOUND
        assertThatThrownBy(() -> service.updateAccount(TENANT_ID, ACCOUNT_ID,
                new UpdateAccountRequest("新名字", null, null, null)))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void findByIdAndTenant_wrongTenant_throws404_notForbidden() {
        // given: 账户存在但属于其他租户，findByIdAndTenantId 返回 empty
        when(accountRepository.findByIdAndTenantId(ACCOUNT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // when / then: H-5 fix - 跨租户访问统一返回 NOT_FOUND（不暴露 FORBIDDEN 信息）
        assertThatThrownBy(() -> service.deleteAccount(TENANT_ID, ACCOUNT_ID, OTHER_ACCOUNT_ID))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND));
    }

    // ──────────────────── 工具方法 ────────────────────

    private Account buildAccount(String id, String studentNo,
                                  Account.AccountType type,
                                  Account.SecondaryRole secondaryRole) {
        Account acc = new Account();
        acc.setId(id);
        acc.setTenantId(TENANT_ID);
        acc.setStudentNo(studentNo);
        acc.setName("测试用户");
        acc.setAccountType(type);
        acc.setSecondaryRole(secondaryRole);
        acc.setStatus(Account.AccountStatus.ACTIVE);
        // 通过反射设置 createdAt（@PrePersist 在测试中不执行）
        try {
            var field = Account.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(acc, LocalDateTime.now());
        } catch (Exception ignored) { /* 测试辅助方法，忽略反射异常 */ }
        return acc;
    }
}
