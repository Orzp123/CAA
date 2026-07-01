package com.caa.school.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

/**
 * M-5 fix: SSO client_secret AES-GCM 加密服务。
 * 使用 Spring Security Crypto TextEncryptor（AES-256-GCM）。
 * 密钥从 app.encryption.secret 读取，盐值随机生成并附加在密文前缀中。
 *
 * 密文格式：{salt_hex}:{encrypted_hex}（由 Encryptors.delux 生成）
 * 存储长度：最大约 200 字符，tenant_sso_configs.client_secret VARCHAR(1024) 足够容纳。
 */
@Service
public class SsoConfigEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(SsoConfigEncryptionService.class);

    /** 仅用于本地开发的默认密钥，生产环境必须通过 APP_ENCRYPTION_SECRET 覆盖。 */
    private static final String DEV_DEFAULT_SECRET = "dev-only-change-in-production-32c";

    private final String secret;

    public SsoConfigEncryptionService(
            @Value("${app.encryption.secret}") String secret,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        if (DEV_DEFAULT_SECRET.equals(secret)) {
            if (activeProfile.contains("prod")) {
                throw new IllegalStateException(
                        "生产环境检测到默认加密密钥，必须设置环境变量 APP_ENCRYPTION_SECRET");
            }
            log.warn("⚠️  使用默认加密密钥（仅限本地开发），生产环境必须设置 APP_ENCRYPTION_SECRET 环境变量");
        }
        this.secret = secret;
    }

    /**
     * 加密明文 secret。每次调用生成随机盐，确保相同明文产生不同密文。
     *
     * @param plainText 明文（如 OAuth2 client_secret）
     * @return AES-GCM 加密后的 Base64 密文（可安全存入数据库）
     */
    public String encrypt(String plainText) {
        String salt = KeyGenerators.string().generateKey();
        TextEncryptor encryptor = Encryptors.delux(secret, salt);
        return salt + ":" + encryptor.encrypt(plainText);
    }

    /**
     * 解密密文。
     *
     * @param cipherText encrypt() 返回的密文（格式：{salt}:{encrypted}）
     * @return 原始明文
     * @throws IllegalArgumentException 格式不合法时抛出
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || !cipherText.contains(":")) {
            throw new IllegalArgumentException("无效的加密格式，期望 {salt}:{encrypted}");
        }
        int idx = cipherText.indexOf(':');
        String salt = cipherText.substring(0, idx);
        String encrypted = cipherText.substring(idx + 1);
        TextEncryptor encryptor = Encryptors.delux(secret, salt);
        return encryptor.decrypt(encrypted);
    }

    /**
     * 判断字符串是否为已加密格式（简单启发式检测）。
     * 用于兼容存量明文数据迁移场景。
     *
     * @param value 待检测字符串
     * @return true 表示已加密
     */
    public boolean isEncrypted(String value) {
        return value != null && value.contains(":");
    }
}
