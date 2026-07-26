package com.passwordleakdetector.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    // 32 raw bytes, base64-encoded - a valid AES-256 key for tests only.
    private static final String TEST_KEY =
            Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());

    private final EncryptionService encryptionService = new EncryptionService(TEST_KEY);

    @Test
    void decryptReversesEncrypt() {
        String plaintext = "correct horse battery staple";

        EncryptionService.EncryptedValue encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted.ciphertext(), encrypted.iv());

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void eachEncryptionUsesADistinctIv() {
        String plaintext = "same-input-every-time";

        EncryptionService.EncryptedValue first = encryptionService.encrypt(plaintext);
        EncryptionService.EncryptedValue second = encryptionService.encrypt(plaintext);

        assertThat(first.iv()).isNotEqualTo(second.iv());
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
    }

    @Test
    void tamperedCiphertextFailsToDecrypt() {
        EncryptionService.EncryptedValue encrypted = encryptionService.encrypt("sensitive-value");
        byte[] rawCiphertext = Base64.getDecoder().decode(encrypted.ciphertext());
        rawCiphertext[0] ^= 0x01; // flip a bit
        String tamperedCiphertext = Base64.getEncoder().encodeToString(rawCiphertext);

        assertThatThrownBy(() -> encryptionService.decrypt(tamperedCiphertext, encrypted.iv()))
                .isInstanceOf(com.passwordleakdetector.exception.EncryptionException.class);
    }

    @Test
    void wrongIvFailsToDecrypt() {
        EncryptionService.EncryptedValue first = encryptionService.encrypt("value-one");
        EncryptionService.EncryptedValue second = encryptionService.encrypt("value-two");

        assertThatThrownBy(() -> encryptionService.decrypt(first.ciphertext(), second.iv()))
                .isInstanceOf(com.passwordleakdetector.exception.EncryptionException.class);
    }

    @Test
    void constructorRejectsKeyOfWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString("too-short-key".getBytes());
        assertThatThrownBy(() -> new EncryptionService(shortKey))
                .isInstanceOf(IllegalStateException.class);
    }
}
