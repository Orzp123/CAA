package com.caa.auth.service;

import com.caa.auth.config.JwtConfig;
import com.caa.auth.dto.TokenInfo;
import com.caa.auth.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link TokenService}.
 * No Spring context is loaded — TokenService is constructed directly.
 */
class TokenServiceTest {

    private static final String TEST_SECRET = "test-secret-32-chars-padding-ok!";
    private static final long   EXPIRY_SECONDS = 3600L;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret(TEST_SECRET);
        config.setExpirationSeconds(EXPIRY_SECONDS);
        tokenService = new TokenService(config);
    }

    // ------------------------------------------------------------------
    // issue()
    // ------------------------------------------------------------------

    @Test
    void issue_returnsNonNullToken() {
        TokenInfo info = tokenService.issue("acc-1", "tenant-1",
                Account.AccountType.TEACHER, null);

        assertThat(info.token()).isNotBlank();
    }

    @Test
    void issue_setsCorrectSubject() {
        TokenInfo info = tokenService.issue("acc-1", "tenant-1",
                Account.AccountType.STUDENT, "S001");

        Jwt jwt = tokenService.parse(info.token());
        assertThat(jwt.getSubject()).isEqualTo("acc-1");
    }

    @Test
    void issue_setsTenantIdClaim() {
        TokenInfo info = tokenService.issue("acc-2", "tenant-42",
                Account.AccountType.SCHOOL_ADMIN, null);

        Jwt jwt = tokenService.parse(info.token());
        assertThat(jwt.<String>getClaim("tenantId")).isEqualTo("tenant-42");
    }

    @Test
    void issue_setsAccountTypeClaim() {
        TokenInfo info = tokenService.issue("acc-3", "tenant-1",
                Account.AccountType.SYSTEM_ADMIN, null);

        Jwt jwt = tokenService.parse(info.token());
        assertThat(jwt.<String>getClaim("accountType"))
                .isEqualTo(Account.AccountType.SYSTEM_ADMIN.name());
    }

    @Test
    void issue_setsNonNullJti() {
        TokenInfo info = tokenService.issue("acc-4", "tenant-1",
                Account.AccountType.TEACHER, null);

        assertThat(info.jti()).isNotBlank();
        // JTI in TokenInfo must match the jti claim inside the token
        Jwt jwt = tokenService.parse(info.token());
        assertThat(jwt.getId()).isEqualTo(info.jti());
    }

    @Test
    void issue_expiryIsRoughlyExpirationSecondsInFuture() {
        Instant before = Instant.now();
        TokenInfo info = tokenService.issue("acc-5", "tenant-1",
                Account.AccountType.STUDENT, "S002");
        Instant after = Instant.now();

        Instant expectedMin = before.plusSeconds(EXPIRY_SECONDS).minusSeconds(5);
        Instant expectedMax = after.plusSeconds(EXPIRY_SECONDS).plusSeconds(5);

        assertThat(info.expiresAt())
                .isAfterOrEqualTo(expectedMin)
                .isBeforeOrEqualTo(expectedMax);
    }

    @Test
    void issue_studentNoClaimPreserved() {
        TokenInfo info = tokenService.issue("acc-6", "tenant-1",
                Account.AccountType.STUDENT, "S999");

        Jwt jwt = tokenService.parse(info.token());
        assertThat(jwt.<String>getClaim("studentNo")).isEqualTo("S999");
    }

    // ------------------------------------------------------------------
    // parse()
    // ------------------------------------------------------------------

    @Test
    void parse_returnsValidJwtForFreshlyIssuedToken() {
        TokenInfo info = tokenService.issue("acc-7", "tenant-1",
                Account.AccountType.TEACHER, null);

        Jwt jwt = tokenService.parse(info.token());

        assertThat(jwt).isNotNull();
        assertThat(jwt.getSubject()).isEqualTo("acc-7");
    }

    @Test
    void parse_throwsJwtExceptionForTamperedToken() {
        TokenInfo info = tokenService.issue("acc-8", "tenant-1",
                Account.AccountType.TEACHER, null);

        // Corrupt the signature (last 10 chars of the compact token)
        String token = info.token();
        String tampered = token.substring(0, token.length() - 10) + "XXXXXXXXXX";

        assertThatThrownBy(() -> tokenService.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtExceptionForGarbageInput() {
        assertThatThrownBy(() -> tokenService.parse("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }
}
