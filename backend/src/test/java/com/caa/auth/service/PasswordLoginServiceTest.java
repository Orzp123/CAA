package com.caa.auth.service;

import com.caa.auth.exception.AccountDisabledException;
import com.caa.auth.exception.AccountLockedException;
import com.caa.auth.exception.AccountNotFoundException;
import com.caa.auth.exception.CaptchaInvalidException;
import com.caa.auth.exception.InvalidCredentialsException;
import com.caa.auth.model.Account;
import com.caa.auth.model.Account.AccountStatus;
import com.caa.auth.model.SystemConfig;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.SystemConfigRepository;
import com.caa.auth.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PasswordLoginService}.
 * No Spring context — all dependencies mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordLoginServiceTest {

    @Mock private CaptchaService captchaService;
    @Mock private TenantRepository tenantRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    private PasswordLoginService service;

    // Test constants (synthetic values only)
    private static final String TENANT_CODE   = "school-a";
    private static final String TENANT_ID     = "tenant-uuid-001";
    private static final String STUDENT_NO    = "S001";
    private static final String RAW_PASSWORD  = "secret123";
    private static final String CAPTCHA_UUID  = "uuid-abc";
    private static final String CAPTCHA_CODE  = "AB12";

    @BeforeEach
    void setUp() {
        service = new PasswordLoginService(
                captchaService,
                tenantRepository,
                accountRepository,
                systemConfigRepository,
                passwordEncoder
        );
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private Tenant activeTenant() {
        Tenant t = new Tenant();
        t.setId(TENANT_ID);
        t.setCode(TENANT_CODE);
        t.setName("School A");
        t.setType(Tenant.TenantType.SCHOOL);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        return t;
    }

    private Account activeAccount(String passwordHash) {
        Account a = new Account();
        a.setId("acc-uuid-001");
        a.setTenantId(TENANT_ID);
        a.setStudentNo(STUDENT_NO);
        a.setName("Test Student");
        a.setAccountType(Account.AccountType.STUDENT);
        a.setStatus(AccountStatus.ACTIVE);
        a.setPasswordHash(passwordHash);
        a.setLoginFailCount(0);
        return a;
    }

    private void stubConfig(String key, String value) {
        SystemConfig cfg = new SystemConfig();
        cfg.setConfigKey(key);
        cfg.setConfigValue(value);
        when(systemConfigRepository.findById(key)).thenReturn(Optional.of(cfg));
    }

    private void stubDefaultConfigs() {
        stubConfig("login.max_fail_count",       "3");
        stubConfig("login.lock_duration_seconds", "600");
    }

    // -----------------------------------------------------------------------
    // captcha validation
    // -----------------------------------------------------------------------

    @Test
    void login_throwsCaptchaInvalidException_whenCaptchaFails() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(false);

        assertThatThrownBy(() ->
                service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE))
                .isInstanceOf(CaptchaInvalidException.class);
    }

    // -----------------------------------------------------------------------
    // tenant / account lookup
    // -----------------------------------------------------------------------

    @Test
    void login_throwsAccountNotFoundException_whenTenantNotFound() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void login_throwsAccountNotFoundException_whenAccountNotFound() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // -----------------------------------------------------------------------
    // disabled account
    // -----------------------------------------------------------------------

    @Test
    void login_throwsAccountDisabledException_whenAccountIsDisabled() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));

        Account disabled = activeAccount("$2a$hash");
        disabled.setStatus(AccountStatus.DISABLED);
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() ->
                service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE))
                .isInstanceOf(AccountDisabledException.class);
    }

    // -----------------------------------------------------------------------
    // locked account
    // -----------------------------------------------------------------------

    @Test
    void login_throwsAccountLockedException_whenAccountLockedWithFutureLockedUntil() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));

        Account locked = activeAccount("$2a$hash");
        locked.setStatus(AccountStatus.LOCKED);
        locked.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.of(locked));

        assertThatThrownBy(() ->
                service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void login_autoUnlocksAndProceedsWhenLockedUntilIsInPast() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));
        stubDefaultConfigs();

        Account locked = activeAccount("$2a$hash");
        locked.setStatus(AccountStatus.LOCKED);
        locked.setLockedUntil(LocalDateTime.now().minusMinutes(5)); // past
        locked.setLoginFailCount(3);
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.of(locked));
        when(passwordEncoder.matches(RAW_PASSWORD, "$2a$hash")).thenReturn(true);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE);

        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getLoginFailCount()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // wrong password — fail count increment
    // -----------------------------------------------------------------------

    @Test
    void login_incrementsLoginFailCount_onWrongPassword() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));
        stubDefaultConfigs();

        Account account = activeAccount("$2a$hash");
        account.setLoginFailCount(1);
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, "$2a$hash")).thenReturn(false);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() ->
                service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getLoginFailCount()).isEqualTo(2);
    }

    @Test
    void login_locksAccount_whenFailCountReachesThreshold() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));
        stubDefaultConfigs();

        Account account = activeAccount("$2a$hash");
        account.setLoginFailCount(2); // one more wrong → 3 = threshold
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, "$2a$hash")).thenReturn(false);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() ->
                service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(saved.getLockedUntil()).isNotNull()
                .isAfter(LocalDateTime.now());
    }

    // -----------------------------------------------------------------------
    // successful login
    // -----------------------------------------------------------------------

    @Test
    void login_resetsFailCountOnSuccess() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));
        stubDefaultConfigs();

        Account account = activeAccount("$2a$hash");
        account.setLoginFailCount(1);
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, "$2a$hash")).thenReturn(true);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE);

        assertThat(result.getLoginFailCount()).isEqualTo(0);
        verify(accountRepository).save(any());
    }

    @Test
    void login_returnsAccountOnSuccess() {
        when(captchaService.verify(CAPTCHA_UUID, CAPTCHA_CODE)).thenReturn(true);
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(activeTenant()));
        stubDefaultConfigs();

        Account account = activeAccount("$2a$hash");
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, STUDENT_NO))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, "$2a$hash")).thenReturn(true);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.login(TENANT_CODE, STUDENT_NO, RAW_PASSWORD, CAPTCHA_UUID, CAPTCHA_CODE);

        assertThat(result).isNotNull();
        assertThat(result.getStudentNo()).isEqualTo(STUDENT_NO);
    }
}
