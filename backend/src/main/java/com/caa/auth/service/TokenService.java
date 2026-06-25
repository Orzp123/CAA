package com.caa.auth.service;

import com.caa.auth.config.JwtConfig;
import com.caa.auth.dto.TokenInfo;
import com.caa.auth.model.Account;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Signs and verifies JWTs using nimbus-jose-jwt directly.
 *
 * <p>Intentionally bypasses the Spring Security filter-chain decoder so that
 * login endpoints can both <em>issue</em> and <em>verify</em> tokens in the
 * same service layer without a circular dependency on the resource-server
 * auto-configuration.
 *
 * <p>The encoder and decoder are rebuilt whenever {@link JwtConfig} changes
 * (Nacos {@code @RefreshScope} triggers a new bean instance, which in turn
 * causes callers that hold a reference to {@code JwtConfig} to rebuild on
 * the next call via {@link #encoder()} / {@link #decoder()}).
 */
@Service
public class TokenService {

    private final JwtConfig jwtConfig;

    public TokenService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Issues a signed JWT for the given account.
     *
     * @param accountId   UUID string of the account
     * @param tenantId    UUID string of the tenant
     * @param accountType account type enum value
     * @param studentNo   student number (may be null for non-student accounts)
     * @return {@link TokenInfo} containing the compact token, JTI, and expiry
     */
    public TokenInfo issue(String accountId,
                           String tenantId,
                           Account.AccountType accountType,
                           String studentNo) {

        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtConfig.getExpirationSeconds());

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .id(jti)
                .subject(accountId)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("tenantId", tenantId)
                .claim("accountType", accountType.name());
        if (studentNo != null) {
            claimsBuilder.claim("studentNo", studentNo);
        }
        JwtClaimsSet claims = claimsBuilder.build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder().encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenInfo(token, jti, expiresAt);
    }

    /**
     * Parses and validates a JWT compact string.
     *
     * @param token compact JWT
     * @return decoded {@link Jwt}
     * @throws JwtException if the token is invalid, tampered, or expired
     */
    public Jwt parse(String token) {
        return decoder().decode(token);
    }

    // -------------------------------------------------------------------------
    // Internal helpers — rebuilt from current JwtConfig on every call so that
    // Nacos hot-reload is honoured without requiring a restart.
    // -------------------------------------------------------------------------

    private JwtEncoder encoder() {
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey().getEncoded())
                .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    private NimbusJwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).build();
    }

    private SecretKey secretKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
