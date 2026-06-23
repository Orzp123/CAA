package com.caa.auth.service;

import com.caa.auth.dto.LoginResponse;
import com.caa.auth.dto.TokenInfo;
import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.TenantRepository;
import com.caa.auth.service.sso.SsoOidcService;
import com.caa.auth.service.wechat.RequiresProfileCompletionException;
import com.caa.auth.service.wechat.WechatOAuth2Service;
import com.caa.auth.service.wechat.WechatTokenResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates all login flows: password, WeChat OAuth2, SSO OIDC.
 * Issues JWT via TokenService and enforces single-device policy.
 */
@Service
public class AuthService {

    private static final String DEFAULT_DEVICE_TYPE = "WEB";

    private final PasswordLoginService  passwordLoginService;
    private final WechatOAuth2Service   wechatOAuth2Service;
    private final SsoOidcService        ssoOidcService;
    private final TokenService          tokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SingleDeviceService   singleDeviceService;
    private final TenantRepository      tenantRepository;

    public AuthService(PasswordLoginService passwordLoginService,
                       WechatOAuth2Service wechatOAuth2Service,
                       SsoOidcService ssoOidcService,
                       TokenService tokenService,
                       TokenBlacklistService tokenBlacklistService,
                       SingleDeviceService singleDeviceService,
                       TenantRepository tenantRepository) {
        this.passwordLoginService  = passwordLoginService;
        this.wechatOAuth2Service   = wechatOAuth2Service;
        this.ssoOidcService        = ssoOidcService;
        this.tokenService          = tokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.singleDeviceService   = singleDeviceService;
        this.tenantRepository      = tenantRepository;
    }

    // -------------------------------------------------------------------------
    // Password login
    // -------------------------------------------------------------------------

    public LoginResponse loginWithPassword(String tenantCode,
                                           String studentNo,
                                           String rawPassword,
                                           String captchaUuid,
                                           String captchaCode) {
        Account account = passwordLoginService.login(
                tenantCode, studentNo, rawPassword, captchaUuid, captchaCode);
        return issueToken(account);
    }

    // -------------------------------------------------------------------------
    // WeChat OAuth2 login
    // -------------------------------------------------------------------------

    /**
     * Handles WeChat login for existing accounts.
     * Returns null when account needs profile completion (first-time login).
     */
    public LoginResponse loginWithWechat(String code) {
        WechatTokenResult wtr = wechatOAuth2Service.exchangeCode(code);
        try {
            Account account = wechatOAuth2Service.findOrCreateAccount(
                    wtr.openid(), wtr.unionid());
            return issueToken(account);
        } catch (RequiresProfileCompletionException e) {
            wechatOAuth2Service.generateTempToken(e.getOpenid());
            return null;
        }
    }

    /**
     * First-time WeChat login: exchanges code and returns a tempToken for profile completion.
     */
    public String loginWithWechatGetTempToken(String code) {
        WechatTokenResult wtr = wechatOAuth2Service.exchangeCode(code);
        try {
            wechatOAuth2Service.findOrCreateAccount(wtr.openid(), wtr.unionid());
            return null;
        } catch (RequiresProfileCompletionException e) {
            return wechatOAuth2Service.generateTempToken(e.getOpenid());
        }
    }

    // -------------------------------------------------------------------------
    // SSO OIDC login
    // -------------------------------------------------------------------------

    public LoginResponse loginWithSso(String tenantCode,
                                      String ssoSubject,
                                      Map<String, Object> claims) {
        Account account = ssoOidcService.findOrCreateAccount(tenantCode, ssoSubject, claims);
        return issueToken(account);
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    public void logout(String token) {
        Jwt jwt = tokenService.parse(token);
        String jti = jwt.getId();
        Instant expiresAt = jwt.getExpiresAt();
        Duration remaining = expiresAt != null
                ? Duration.between(Instant.now(), expiresAt)
                : Duration.ofHours(2);
        if (!remaining.isNegative()) {
            tokenBlacklistService.addToBlacklist(jti, remaining);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private LoginResponse issueToken(Account account) {
        TokenInfo tokenInfo = tokenService.issue(
                account.getId(),
                account.getTenantId(),
                account.getAccountType(),
                account.getStudentNo());

        handleSingleDevice(account, tokenInfo);

        Tenant tenant = tenantRepository.findById(account.getTenantId()).orElse(null);
        String tenantName = tenant != null ? tenant.getName() : "";
        String nickname = account.getNickname() != null
                ? account.getNickname() : account.getName();

        return new LoginResponse(
                tokenInfo.token(),
                tokenInfo.expiresAt(),
                account.getId(),
                nickname,
                account.getAccountType(),
                account.getTenantId(),
                tenantName);
    }

    private void handleSingleDevice(Account account, TokenInfo tokenInfo) {
        if (!singleDeviceService.isEnabled(account.getTenantId(), account.getAccountType())) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), tokenInfo.expiresAt());
        Optional<String> oldJti = singleDeviceService.trackLogin(
                account.getId(), DEFAULT_DEVICE_TYPE, tokenInfo.jti(), ttl);
        oldJti.ifPresent(jti -> tokenBlacklistService.addToBlacklist(jti, ttl));
    }
}
