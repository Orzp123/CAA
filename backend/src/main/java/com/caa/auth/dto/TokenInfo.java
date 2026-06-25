package com.caa.auth.dto;

import java.time.Instant;

/**
 * Issued JWT descriptor returned by {@link com.caa.auth.service.TokenService#issue}.
 *
 * @param token     signed JWT compact string
 * @param jti       JWT ID (UUID), usable as blacklist key
 * @param expiresAt absolute expiry instant (= iat + expirationSeconds)
 */
public record TokenInfo(String token, String jti, Instant expiresAt) {}
