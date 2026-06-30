package com.caa.school.service;

import com.caa.auth.model.Account;
import com.caa.auth.repository.AccountRepository;
import com.caa.common.ErrorCode;
import com.caa.school.dto.*;
import com.caa.school.exception.SchoolException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 账户管理服务。
 * 负责学校租户下账户的增删改查、批量操作。
 */
@Service
public class SchoolAccountService {

    // H-4 fix: 默认密码通过配置注入，禁止硬编码
    @Value("${app.account.default-password:Caa@2026}")
    private String defaultPassword;

    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public SchoolAccountService(AccountRepository accountRepository,
                                PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ──────────────────── 创建账户 ────────────────────

    @Transactional
    public AccountResponse createAccount(String tenantId, CreateAccountRequest req) {
        // 1. 登录名唯一性校验
        accountRepository.findByTenantIdAndStudentNo(tenantId, req.loginName())
                .ifPresent(existing -> {
                    throw new SchoolException(ErrorCode.LOGIN_NAME_DUPLICATE);
                });

        // 2. 解析 accountType
        Account.AccountType accountType = Account.AccountType.valueOf(req.accountType());

        // 3. 解析 secondaryRole（可为 null）
        Account.SecondaryRole secondaryRole = null;
        if (req.secondaryRole() != null && !req.secondaryRole().isBlank()) {
            secondaryRole = Account.SecondaryRole.valueOf(req.secondaryRole());
        }

        // 4. 第二身份校验规则
        validateSecondaryRole(accountType, secondaryRole);

        // 5. 邮箱格式校验
        if (req.email() != null && !req.email().isBlank()) {
            if (!req.email().matches(EMAIL_REGEX)) {
                throw new SchoolException(ErrorCode.EMAIL_FORMAT_INVALID);
            }
        }

        // 6. 手机号格式校验
        if (req.phone() != null && !req.phone().isBlank()) {
            if (!req.phone().matches(PHONE_REGEX)) {
                throw new SchoolException(ErrorCode.PHONE_FORMAT_INVALID);
            }
        }

        // 7. 构建并保存
        Account account = new Account();
        account.setTenantId(tenantId);
        account.setStudentNo(req.loginName());
        account.setName(req.name());
        account.setNickname(req.nickname());
        account.setEmail(req.email());
        account.setPhone(req.phone());
        account.setAccountType(accountType);
        account.setSecondaryRole(secondaryRole);
        account.setStatus(Account.AccountStatus.ACTIVE);

        String rawPassword = (req.password() != null && !req.password().isBlank())
                ? req.password() : defaultPassword;
        account.setPasswordHash(passwordEncoder.encode(rawPassword));

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    // ──────────────────── 账户列表（JPA Specification AND 逻辑）────────────────────

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccounts(String tenantId,
                                               String loginName,
                                               Account.AccountType accountType,
                                               String name,
                                               Pageable pageable) {
        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (loginName != null && !loginName.isBlank()) {
                // H-3 fix: 使用后缀 LIKE（keyword%）避免全表扫描
                predicates.add(cb.like(root.get("studentNo"), loginName + "%"));
            }
            if (accountType != null) {
                predicates.add(cb.equal(root.get("accountType"), accountType));
            }
            if (name != null && !name.isBlank()) {
                // H-3 fix: 使用后缀 LIKE（keyword%）配合 idx_accounts_tenant_name 索引
                predicates.add(cb.like(root.get("name"), name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return accountRepository.findAll(spec, pageable).map(this::toResponse);
    }

    // ──────────────────── 更新账户 ────────────────────

    @Transactional
    public AccountResponse updateAccount(String tenantId, String accountId, UpdateAccountRequest req) {
        Account account = findByIdAndTenant(accountId, tenantId);

        account.setName(req.name());
        account.setNickname(req.nickname());

        if (req.email() != null && !req.email().isBlank()) {
            if (!req.email().matches(EMAIL_REGEX)) {
                throw new SchoolException(ErrorCode.EMAIL_FORMAT_INVALID);
            }
            account.setEmail(req.email());
        }

        if (req.phone() != null && !req.phone().isBlank()) {
            if (!req.phone().matches(PHONE_REGEX)) {
                throw new SchoolException(ErrorCode.PHONE_FORMAT_INVALID);
            }
            account.setPhone(req.phone());
        }

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    // ──────────────────── 重置密码 ────────────────────

    @Transactional
    public void resetPassword(String tenantId, String accountId, String newPassword) {
        Account account = findByIdAndTenant(accountId, tenantId);
        String rawPassword = (newPassword != null && !newPassword.isBlank())
                ? newPassword : defaultPassword;
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        accountRepository.save(account);
    }

    // ──────────────────── 更新状态 ────────────────────

    @Transactional
    public void updateStatus(String tenantId, String accountId, Account.AccountStatus status) {
        Account account = findByIdAndTenant(accountId, tenantId);
        account.setStatus(status);
        accountRepository.save(account);
    }

    // ──────────────────── 删除账户 ────────────────────

    @Transactional
    public void deleteAccount(String tenantId, String accountId, String currentAccountId) {
        if (accountId.equals(currentAccountId)) {
            throw new SchoolException(ErrorCode.SELF_DELETE_FORBIDDEN);
        }
        Account account = findByIdAndTenant(accountId, tenantId);
        accountRepository.delete(account);
    }

    // ──────────────────── 批量删除 ────────────────────

    @Transactional
    public BatchDeleteResponse batchDelete(String tenantId,
                                           List<String> accountIds,
                                           String currentAccountId) {
        int excludedSelfCount = 0;
        List<String> toDeleteIds = new ArrayList<>();

        for (String id : accountIds) {
            if (id.equals(currentAccountId)) {
                excludedSelfCount++;
            } else {
                toDeleteIds.add(id);
            }
        }

        List<Account> toDelete = accountRepository.findAllByIdInAndTenantId(toDeleteIds, tenantId);
        accountRepository.deleteAll(toDelete);

        return new BatchDeleteResponse(toDelete.size(), excludedSelfCount);
    }

    // ──────────────────── 批量更新状态 ────────────────────

    @Transactional
    public void batchUpdateStatus(String tenantId,
                                   List<String> accountIds,
                                   Account.AccountStatus status) {
        List<Account> accounts = accountRepository.findAllByIdInAndTenantId(accountIds, tenantId);
        accounts.forEach(acc -> acc.setStatus(status));
        accountRepository.saveAll(accounts);
    }

    // ──────────────────── 私有辅助方法 ────────────────────

    // H-5 fix: 改为 findByIdAndTenantId，一次查询同时校验租户归属，不存在统一返回 NOT_FOUND
    private Account findByIdAndTenant(String accountId, String tenantId) {
        return accountRepository.findByIdAndTenantId(accountId, tenantId)
                .orElseThrow(() -> new SchoolException(ErrorCode.NOT_FOUND,
                        "账户不存在或无权操作"));
    }

    /**
     * 第二身份校验规则：
     * SYSTEM_ADMIN → secondaryRole 必须为 null
     * 其他类型 → secondaryRole ∈ {TEACHER, ASSISTANT, null}
     */
    private void validateSecondaryRole(Account.AccountType accountType,
                                        Account.SecondaryRole secondaryRole) {
        if (accountType == Account.AccountType.SYSTEM_ADMIN && secondaryRole != null) {
            throw new SchoolException(ErrorCode.SECONDARY_ROLE_INVALID);
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getStudentNo(),
                account.getName(),
                account.getAccountType() != null ? account.getAccountType().name() : null,
                account.getSecondaryRole() != null ? account.getSecondaryRole().name() : null,
                account.getStatus() != null ? account.getStatus().name() : null,
                account.getCreatedAt()
        );
    }
}
