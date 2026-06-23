package com.caa.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new TokenBlacklistService(redisTemplate);
    }

    @Test
    void addToBlacklist_setsKeyWithCorrectTtl() {
        String jti = "abc-123";
        Duration ttl = Duration.ofMinutes(30);

        service.addToBlacklist(jti, ttl);

        verify(valueOps).set("token:blacklist:abc-123", "1", ttl);
    }

    @Test
    void isBlacklisted_returnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey("token:blacklist:abc-123")).thenReturn(Boolean.TRUE);

        boolean result = service.isBlacklisted("abc-123");

        assertThat(result).isTrue();
    }

    @Test
    void isBlacklisted_returnsFalseWhenKeyMissing() {
        when(redisTemplate.hasKey("token:blacklist:xyz-999")).thenReturn(Boolean.FALSE);

        boolean result = service.isBlacklisted("xyz-999");

        assertThat(result).isFalse();
    }

    @Test
    void isBlacklisted_returnsFalseWhenRedisReturnsNull() {
        when(redisTemplate.hasKey("token:blacklist:null-case")).thenReturn(null);

        boolean result = service.isBlacklisted("null-case");

        assertThat(result).isFalse();
    }
}
