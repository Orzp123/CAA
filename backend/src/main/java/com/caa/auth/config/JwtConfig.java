package com.caa.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration bound to the {@code jwt.*} namespace.
 * Annotated with {@code @RefreshScope} so Nacos hot-reload propagates
 * new secret / expiry values without a restart.
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String algorithm = "HS256";
    private String secret = "default-secret-must-override";
    private long expirationSeconds = 7200;

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationSeconds() { return expirationSeconds; }
    public void setExpirationSeconds(long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
}
