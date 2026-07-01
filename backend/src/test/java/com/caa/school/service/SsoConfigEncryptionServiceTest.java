package com.caa.school.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * M-5: SsoConfigEncryptionService 单元测试。
 * 验证 AES-GCM 加密/解密往返正确性。
 */
class SsoConfigEncryptionServiceTest {

    private static final String TEST_SECRET = "test-secret-32chars-padding-here";

    private SsoConfigEncryptionService service;

    @BeforeEach
    void setUp() {
        // 第二参数传 "dev"，避免触发默认密钥检测的生产环境保护
        service = new SsoConfigEncryptionService(TEST_SECRET, "dev");
    }

    @Test
    void encrypt_decrypt_roundtrip() {
        // given
        String plainText = "my-oauth2-client-secret-value";

        // when
        String cipherText = service.encrypt(plainText);
        String decrypted = service.decrypt(cipherText);

        // then
        assertThat(decrypted).isEqualTo(plainText);
        assertThat(cipherText).isNotEqualTo(plainText);
    }

    @Test
    void encrypt_sameInput_producesDifferentCiphertext() {
        // given: 相同明文两次加密应产生不同密文（随机盐）
        String plainText = "same-secret";

        // when
        String cipher1 = service.encrypt(plainText);
        String cipher2 = service.encrypt(plainText);

        // then
        assertThat(cipher1).isNotEqualTo(cipher2);
    }

    @Test
    void encrypt_containsSaltSeparator() {
        // given
        String plainText = "any-secret";

        // when
        String cipherText = service.encrypt(plainText);

        // then: 密文格式为 {salt}:{encrypted}
        assertThat(cipherText).contains(":");
    }

    @Test
    void isEncrypted_encryptedValue_returnsTrue() {
        // given
        String cipherText = service.encrypt("some-secret");

        // when / then
        assertThat(service.isEncrypted(cipherText)).isTrue();
    }

    @Test
    void isEncrypted_plainValue_returnsFalse() {
        // given: 明文不含冒号
        assertThat(service.isEncrypted("plainpassword")).isFalse();
        assertThat(service.isEncrypted(null)).isFalse();
    }

    @Test
    void decrypt_invalidFormat_throwsException() {
        // given: 格式不含冒号
        assertThatThrownBy(() -> service.decrypt("nocollonhere"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效的加密格式");
    }

    @Test
    void decrypt_nullInput_throwsException() {
        assertThatThrownBy(() -> service.decrypt(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_prodProfile_defaultSecret_throwsException() {
        // given: 生产环境使用默认密钥应抛出异常
        assertThatThrownBy(() ->
                new SsoConfigEncryptionService("dev-only-change-in-production-32c", "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("生产环境");
    }
}
