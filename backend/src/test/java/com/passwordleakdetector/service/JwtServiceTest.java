package com.passwordleakdetector.service;

import com.passwordleakdetector.entity.User;
import com.passwordleakdetector.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(SECRET, 15);

    private User userWith(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("irrelevant-for-this-test");
        return user;
    }

    @Test
    void generatedTokenIsValidAndCarriesUsername() {
        User user = userWith(1L, "alice");

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void tamperedTokenIsRejected() {
        User user = userWith(1L, "alice");
        String token = jwtService.generateAccessToken(user);

        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(jwtService.isValid("not-a-jwt-at-all")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 0);
        User user = userWith(2L, "bob");

        String token = shortLivedJwtService.generateAccessToken(user);
        Thread.sleep(50);

        assertThat(shortLivedJwtService.isValid(token)).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        User user = userWith(1L, "alice");
        JwtService otherJwtService = new JwtService("a-completely-different-32-byte-secret!!", 15);

        String token = otherJwtService.generateAccessToken(user);

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void constructorRejectsSecretShorterThan32Bytes() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short", 15));
    }
}
