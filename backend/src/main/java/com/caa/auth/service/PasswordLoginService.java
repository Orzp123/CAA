package com.caa.auth.service;

import com.caa.auth.exception.AccountDisabledException;
import com.caa.auth.exception.AccountLockedException;
import com.caa.auth.exception.AccountNotFoundException;
import com.caa.auth.exception.CaptchaInvalidException;
import com.caa.auth.exception.InvalidCredentialsException;
import com.caa.auth.model.Account;
import com.caa.auth.model.Account.AccountStatus;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.SystemConfigRepository;
import com.caa.auth.repository.TenantRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles password-based login:
 * captcha → tenant → account → lock/disabled check → BCrypt → fail-count management.
 */
@Service
public class PasswordLoginService {

    private static final String KEY_MAX_FAIL    = "login.max_fail_count";
    private static final String KEY_LOCK_SECS   = "login.lock_duration_seconds";
    private static final String DEFAULT_MAX_FAIL = "5";
    private static final String DEFAULT_LOCK_SECS = "600";

    private final CaptchaService captchaService;
    private final TenantRepository tenantRepository;
    private final AccountRepository accountRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordLoginService(CaptchaService captchaService,
                                TenantRepository tenantRepository,
                                AccountRepository accountRepository,
                                SystemConfigRepository systemConfigRepository,
                                BCryptPasswordEncoder passwordEncoder) {
        this.captchaService = captchaService;
        this.tenantRepository = tenantRepository;
        this.accountRepository = accountRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Attempts a password login.
     *
     * @param tenantCode   tenant short code
     * @param studentNo    student/staff number within the tenant
     * @param rawPassword  plaintext password supplied by the user
     * @param captchaUuid  UUID identifying the captcha challenge
     * @param captchaCode  user-supplied captcha answer
     * @return the authenticated {@link Account} (failCount reset, saved)
     * @throws CaptchaInvalidException      if captcha verification fails
     * @throws AccountNotFoundException     if tenant or account does not exist
     * @throws AccountDisabledException     if the account is DISABLED
     * @throws AccountLockedException       if the account is LOCKED and not yet expired
     * @throws InvalidCredentialsException  if the password does not match
     */
    public Account login(String tenantCode,
                         String studentNo,
                         String rawPassword,
                         String captchaUuid,
                         String captchaCode) {

        // 1. Verify captcha
        if (!captchaService.verify(captchaUuid, captchaCode)) {
            throw new CaptchaInvalidException();
        }

        // 2. Resolve tenant
        String tenantId = tenantRepository.findByCode(tenantCode)
                .orElseThrow(AccountNotFoundException::new)
                .getId();

        // 3. Resolve account
        Account account = accountRepository
                .findByTenantIdAndStudentNo(tenantId, studentNo)
                .orElseThrow(AccountNotFoundException::new);

        // 4. Disabled check
        if (account.getStatus() == AccountStatus.DISABLED) {
            throw new AccountDisabledException();
        }

        // 5. Locked check — auto-unlock if lock has expired
        if (account.getStatus() == AccountStatus.LOCKED) {
            LocalDateTime lockedUntil = account.getLockedUntil();
            if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
                throw new AccountLockedException(lockedUntil);
            }
            // Lock expired — reset and continue
            account.setStatus(AccountStatus.ACTIVE);
            account.setLoginFailCount(0);
            account.setLockedUntil(null);
        }

        // 6. Password verification
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            handleFailedAttempt(account);
            throw new InvalidCredentialsException();
        }

        // 7. Success — reset fail count
        account.setLoginFailCount(0);
        return accountRepository.save(account);
    }

    // -----------------------------------------------------------------------
    // private helpers
    // -----------------------------------------------------------------------

    private void handleFailedAttempt(Account account) {
        int maxFail = Integer.parseInt(
                systemConfigRepository.findById(KEY_MAX_FAIL)
                        .map(c -> c.getConfigValue())
                        .orElse(DEFAULT_MAX_FAIL));

        int lockSecs = Integer.parseInt(
                systemConfigRepository.findById(KEY_LOCK_SECS)
                        .map(c -> c.getConfigValue())
                        .orElse(DEFAULT_LOCK_SECS));

        int newCount = account.getLoginFailCount() + 1;
        account.setLoginFailCount(newCount);

        if (newCount >= maxFail) {
            account.setStatus(AccountStatus.LOCKED);
            account.setLockedUntil(LocalDateTime.now().plusSeconds(lockSecs));
        }

        accountRepository.save(account);
    }
}
