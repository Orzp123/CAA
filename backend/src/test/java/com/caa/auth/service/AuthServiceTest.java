package com.caa.auth.service;

import com.caa.auth.dto.LoginResponse;
import com.caa.auth.dto.TokenInfo;
import com.caa.auth.exception.AccountDisabledException;
import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.TenantRepository;
import com.caa.auth.service.sso.SsoOidcService;
import com.caa.auth.service.wechat.RequiresProfileCompletionException;
import com.caa.auth.service.wechat.WechatOAuth2Service;
import com.caa.auth.service.wechat.WechatTokenResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock PasswordLoginService  passwordLoginService;
    @Mock WechatOAuth2Service   wechatOAuth2Service;
    @Mock SsoOidcService        ssoOidcService;
    @Mock TokenService          tokenService;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock SingleDeviceService   singleDeviceService;
    @Mock TenantRepository      tenantRepository;

    AuthService authService;

    private static final String  ACCOUNT_ID  = "acc-uuid-1";
    private static final String  TENANT_ID   = "ten-uuid-1";
    private static final String  TENANT_NAME = "北京大学";
    private static final String  TOKEN       = "eyJhbGciOiJIUzI1NiJ9.test";
    private static final Instant EXPIRES_AT  = Instant.now().plusSeconds(7200);

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                passwordLoginService, wechatOAuth2Service, ssoOidcService,
                tokenService, tokenBlacklistService, singleDeviceService,
                tenantRepository);
    }

    private Account buildAccount() {
        Account a = new Account();
        a.setId(ACCOUNT_ID);
        a.setTenantId(TENANT_ID);
        a.setStudentNo("2024001");
        a.setName("张三");
        a.setNickname("小张");
        a.setAccountType(Account.AccountType.STUDENT);
        a.setStatus(Account.AccountStatus.ACTIVE);
        return a;
    }

    private Tenant buildTenant() {
        Tenant t = new Tenant();
        t.setId(TENANT_ID);
        t.setName(TENANT_NAME);
        t.setCode("pku");
        t.setType(Tenant.TenantType.SCHOOL);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        t.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        return t;
    }

    private TokenInfo buildTokenInfo() {
        return new TokenInfo(TOKEN, "jti-001", EXPIRES_AT);
    }

    @Test
    @DisplayName("loginWithPassword returns LoginResponse with token")
    void loginWithPassword_success() {
        when(passwordLoginService.login("pku", "2024001", "Pass1234", "cap-uuid", "A3K9"))
                .thenReturn(buildAccount());
        when(tokenService.issue(ACCOUNT_ID, TENANT_ID, Account.AccountType.STUDENT, "2024001"))
                .thenReturn(buildTokenInfo());
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(singleDeviceService.isEnabled(TENANT_ID, Account.AccountType.STUDENT)).thenReturn(false);

        LoginResponse resp = authService.loginWithPassword(
                "pku", "2024001", "Pass1234", "cap-uuid", "A3K9");

        assertThat(resp.token()).isEqualTo(TOKEN);
        assertThat(resp.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(resp.tenantName()).isEqualTo(TENANT_NAME);
    }

    @Test
    @DisplayName("loginWithPassword enforces single-device: old JTI blacklisted")
    void loginWithPassword_singleDevice_blacklistsOldJti() {
        when(passwordLoginService.login(any(), any(), any(), any(), any())).thenReturn(buildAccount());
        when(tokenService.issue(any(), any(), any(), any())).thenReturn(buildTokenInfo());
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(singleDeviceService.isEnabled(TENANT_ID, Account.AccountType.STUDENT)).thenReturn(true);
        when(singleDeviceService.trackLogin(eq(ACCOUNT_ID), eq("WEB"), eq("jti-001"), any(Duration.class)))
                .thenReturn(Optional.of("old-jti-999"));

        authService.loginWithPassword("pku", "2024001", "Pass1234", "cap-uuid", "A3K9");

        verify(tokenBlacklistService).addToBlacklist(eq("old-jti-999"), any(Duration.class));
    }

    @Test
    @DisplayName("loginWithPassword propagates AccountDisabledException")
    void loginWithPassword_disabled_throws() {
        when(passwordLoginService.login(any(), any(), any(), any(), any()))
                .thenThrow(new AccountDisabledException());

        assertThatThrownBy(() ->
                authService.loginWithPassword("pku", "2024001", "Pass1234", "u", "c"))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    @DisplayName("loginWithWechat returns LoginResponse for existing account")
    void loginWithWechat_existingAccount() {
        WechatTokenResult wtr = new WechatTokenResult("at-xxx", "openid-abc", "unionid-xyz", null, null);
        when(wechatOAuth2Service.exchangeCode("auth-code-123")).thenReturn(wtr);
        when(wechatOAuth2Service.findOrCreateAccount("openid-abc", "unionid-xyz")).thenReturn(buildAccount());
        when(tokenService.issue(any(), any(), any(), any())).thenReturn(buildTokenInfo());
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(singleDeviceService.isEnabled(any(), any())).thenReturn(false);

        LoginResponse resp = authService.loginWithWechat("auth-code-123");

        assertThat(resp.token()).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("loginWithWechat stores tempToken when account is new")
    void loginWithWechat_newAccount_generatesTempToken() {
        WechatTokenResult wtr = new WechatTokenResult("at-xxx", "openid-new", "unionid-new", null, null);
        when(wechatOAuth2Service.exchangeCode("code-new")).thenReturn(wtr);
        when(wechatOAuth2Service.findOrCreateAccount("openid-new", "unionid-new"))
                .thenThrow(new RequiresProfileCompletionException("openid-new"));
        when(wechatOAuth2Service.generateTempToken("openid-new")).thenReturn("temp-token-abc");

        String tempToken = authService.loginWithWechatGetTempToken("code-new");

        assertThat(tempToken).isEqualTo("temp-token-abc");
    }

    @Test
    @DisplayName("logout adds token JTI to blacklist")
    void logout_blacklistsJti() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "HS256")
                .claim("jti", "jti-logout-001")
                .issuedAt(Instant.now())
                .expiresAt(EXPIRES_AT)
                .build();
        when(tokenService.parse(TOKEN)).thenReturn(jwt);

        authService.logout(TOKEN);

        verify(tokenBlacklistService).addToBlacklist(eq("jti-logout-001"), any(Duration.class));
    }
}
