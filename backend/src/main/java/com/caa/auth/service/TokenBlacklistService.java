package com.caa.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Adds a JWT JTI to the blacklist with the given TTL.
     * Key: token:blacklist:{jti}  Value: "1"
     */
    public void addToBlacklist(String jti, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
    }

    /**
     * Returns true if the JTI is present in the blacklist.
     */
    public boolean isBlacklisted(String jti) {
        Boolean exists = redisTemplate.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}
