package com.caa.auth.service;

import com.google.code.kaptcha.Producer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

/**
 * Generates captcha images backed by kaptcha, stores the code in Redis
 * with a 5-minute TTL, and verifies user input (case-insensitive, one-time).
 */
@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    private static final Duration TTL = Duration.ofSeconds(300);

    private final StringRedisTemplate redisTemplate;
    private final Producer kaptchaProducer;

    public CaptchaService(StringRedisTemplate redisTemplate, Producer kaptchaProducer) {
        this.redisTemplate = redisTemplate;
        this.kaptchaProducer = kaptchaProducer;
    }

    /**
     * Generates a captcha image, stores its code in Redis keyed by {@code uuid},
     * and returns the PNG bytes.
     *
     * @param uuid unique identifier for this captcha instance
     * @return PNG-encoded image bytes
     */
    public byte[] generate(String uuid) {
        String code = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(code);

        redisTemplate.opsForValue().set(KEY_PREFIX + uuid, code, TTL);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode captcha image", e);
        }
    }

    /**
     * Verifies the user-supplied code against the stored code for {@code uuid}.
     * Case-insensitive. Deletes the Redis key on successful verification.
     *
     * @param uuid      the captcha instance identifier
     * @param inputCode the user-supplied code
     * @return {@code true} if the code matches; {@code false} otherwise
     */
    public boolean verify(String uuid, String inputCode) {
        String key = KEY_PREFIX + uuid;
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            return false;
        }

        if (stored.equalsIgnoreCase(inputCode)) {
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }
}
