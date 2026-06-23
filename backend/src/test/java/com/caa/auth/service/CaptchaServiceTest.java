package com.caa.auth.service;

import com.google.code.kaptcha.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.awt.image.BufferedImage;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CaptchaService}.
 * No Spring context — dependencies are mocked.
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private Producer kaptchaProducer;

    private CaptchaService captchaService;

    private static final String UUID = "test-uuid-1234";
    private static final String REDIS_KEY = "captcha:" + UUID;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        captchaService = new CaptchaService(redisTemplate, kaptchaProducer);
    }

    // -----------------------------------------------------------------------
    // generate()
    // -----------------------------------------------------------------------

    @Test
    void generate_storesGeneratedCodeInRedisWithTtl() throws Exception {
        String code = "AB12";
        BufferedImage image = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);
        when(kaptchaProducer.createText()).thenReturn(code);
        when(kaptchaProducer.createImage(code)).thenReturn(image);

        captchaService.generate(UUID);

        verify(valueOps).set(eq(REDIS_KEY), eq(code), eq(Duration.ofSeconds(300)));
    }

    @Test
    void generate_returnsPngBytesThatAreNonEmpty() throws Exception {
        String code = "XY99";
        BufferedImage image = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);
        when(kaptchaProducer.createText()).thenReturn(code);
        when(kaptchaProducer.createImage(code)).thenReturn(image);

        byte[] png = captchaService.generate(UUID);

        assertThat(png).isNotEmpty();
    }

    // -----------------------------------------------------------------------
    // verify()
    // -----------------------------------------------------------------------

    @Test
    void verify_returnsTrueForCorrectCode() {
        when(valueOps.get(REDIS_KEY)).thenReturn("AB12");

        boolean result = captchaService.verify(UUID, "AB12");

        assertThat(result).isTrue();
    }

    @Test
    void verify_isCaseInsensitive() {
        when(valueOps.get(REDIS_KEY)).thenReturn("AB12");

        boolean result = captchaService.verify(UUID, "ab12");

        assertThat(result).isTrue();
    }

    @Test
    void verify_returnsFalseForWrongCode() {
        when(valueOps.get(REDIS_KEY)).thenReturn("AB12");

        boolean result = captchaService.verify(UUID, "ZZZZ");

        assertThat(result).isFalse();
    }

    @Test
    void verify_returnsFalseWhenKeyNotFoundInRedis() {
        when(valueOps.get(REDIS_KEY)).thenReturn(null);

        boolean result = captchaService.verify(UUID, "AB12");

        assertThat(result).isFalse();
    }

    @Test
    void verify_deletesRedisKeyAfterSuccessfulVerification() {
        when(valueOps.get(REDIS_KEY)).thenReturn("AB12");

        captchaService.verify(UUID, "AB12");

        verify(redisTemplate).delete(REDIS_KEY);
    }

    @Test
    void verify_doesNotDeleteRedisKeyAfterFailedVerification() {
        when(valueOps.get(REDIS_KEY)).thenReturn("AB12");

        captchaService.verify(UUID, "WRONG");

        verify(redisTemplate, never()).delete(anyString());
    }
}
