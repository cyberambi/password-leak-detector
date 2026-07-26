package com.passwordleakdetector.service;

import com.passwordleakdetector.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM authenticated encryption for password-history values at rest.
 * The key never lives in code or VCS - it is read once at startup from the
 * AES_KEY environment variable (base64-encoded, must decode to exactly 32 bytes).
 */
@Service
public class EncryptionService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.security.aes-key}") String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("AES_KEY must be valid base64", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "AES_KEY must decode to exactly 32 bytes (256 bits), got " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public EncryptedValue encrypt(String plaintext) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv));
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Failed to encrypt value", e);
        }
    }

    public String decrypt(String base64Ciphertext, String base64Iv) {
        try {
            byte[] iv = Base64.getDecoder().decode(base64Iv);
            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new EncryptionException("Failed to decrypt value", e);
        }
    }

    public record EncryptedValue(String ciphertext, String iv) {
    }
}
