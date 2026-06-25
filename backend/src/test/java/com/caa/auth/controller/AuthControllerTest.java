package com.caa.auth.controller;

import com.caa.auth.dto.LoginRequest;
import com.caa.auth.dto.LoginResponse;
import com.caa.auth.dto.TenantConfigResponse;
import com.caa.auth.exception.AccountDisabledException;
import com.caa.auth.exception.AccountLockedException;
import com.caa.auth.exception.CaptchaInvalidException;
import com.caa.auth.exception.InvalidCredentialsException;
import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.service.AuthService;
import com.caa.auth.service.CaptchaService;
import com.caa.auth.service.TenantService;
import com.caa.auth.service.wechat.WechatOAuth2Service;
import com.caa.common.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({GlobalExceptionHandler.class, JacksonAutoConfiguration.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean AuthService         authService;
    @MockitoBean CaptchaService      captchaService;
    @MockitoBean TenantService       tenantService;
    @MockitoBean WechatOAuth2Service wechatOAuth2Service;

    // ── fixtures ────────────────────────────────────────────────────────────

    private static final String TOKEN      = "eyJ.test.token";
    private static final Instant EXPIRES   = Instant.now().plusSeconds(7200);
    private static final String ACCOUNT_ID = "acc-uuid-1";
    private static final String TENANT_ID  = "ten-uuid-1";
    private static final String TENANT_NAME= "北京大学";

    private LoginResponse loginResponse() {
        return new LoginResponse(
                TOKEN, EXPIRES, ACCOUNT_ID, "小张",
                Account.AccountType.STUDENT, TENANT_ID, TENANT_NAME);
    }

    // ── POST /auth/login ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login — 200 with token on valid credentials")
    void login_success() throws Exception {
        when(authService.loginWithPassword("pku", "2024001", "Pass@1234", "uuid-cap-001", "A3K9"))
                .thenReturn(loginResponse());

        String body = objectMapper.writeValueAsString(
                new LoginRequest("2024001", "pku", "Pass@1234", "uuid-cap-001", "A3K9"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(TOKEN))
                .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID));
    }

    @Test
    @DisplayName("POST /auth/login — 401 when captcha is invalid")
    void login_captchaInvalid() throws Exception {
        when(authService.loginWithPassword(any(), any(), any(), any(), any()))
                .thenThrow(new CaptchaInvalidException());

        String body = objectMapper.writeValueAsString(
                new LoginRequest("2024001", "pku", "Pass@1234", "uuid-cap-001", "WRONG"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ERR_CAPTCHA_INVALID"));
    }

    @Test
    @DisplayName("POST /auth/login — 423 when account is locked")
    void login_accountLocked() throws Exception {
        when(authService.loginWithPassword(any(), any(), any(), any(), any()))
                .thenThrow(new AccountLockedException(LocalDateTime.now().plusMinutes(10)));

        String body = objectMapper.writeValueAsString(
                new LoginRequest("2024001", "pku", "Pass@1234", "uuid-cap-001", "A3K9"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ERR_ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("POST /auth/login — 401 when credentials are invalid")
    void login_invalidCredentials() throws Exception {
        when(authService.loginWithPassword(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidCredentialsException());

        String body = objectMapper.writeValueAsString(
                new LoginRequest("2024001", "pku", "wrongPass", "uuid-cap-001", "A3K9"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ERR_INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /auth/login — 403 when account is disabled")
    void login_accountDisabled() throws Exception {
        when(authService.loginWithPassword(any(), any(), any(), any(), any()))
                .thenThrow(new AccountDisabledException());

        String body = objectMapper.writeValueAsString(
                new LoginRequest("2024001", "pku", "Pass@1234", "uuid-cap-001", "A3K9"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ERR_ACCOUNT_DISABLED"));
    }

    // ── GET /auth/captcha ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/captcha — 200 image/png bytes")
    void captcha_returnsPngBytes() throws Exception {
        byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        when(captchaService.generate("uuid-cap-001")).thenReturn(pngBytes);

        mockMvc.perform(get("/auth/captcha").param("uuid", "uuid-cap-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(pngBytes));
    }

    // ── POST /auth/logout ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/logout — 200 with null data")
    void logout_success() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ── GET /auth/tenant-config ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/tenant-config — 200 with config by tenantCode")
    void tenantConfig_byCode() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName(TENANT_NAME);
        tenant.setCode("pku");
        tenant.setType(Tenant.TenantType.SCHOOL);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setDefaultLoginType(Tenant.LoginType.PASSWORD);

        when(tenantService.findByCode("pku")).thenReturn(tenant);

        mockMvc.perform(get("/auth/tenant-config").param("tenantCode", "pku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantName").value(TENANT_NAME))
                .andExpect(jsonPath("$.data.defaultLoginType").value("PASSWORD"));
    }
}
