package com.passwordleakdetector.security;

import com.passwordleakdetector.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpiryMillis;

    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                       @Value("${app.security.access-token-expiry-minutes}") long accessTokenExpiryMinutes) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes long (256 bits) for HS256 signing");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenExpiryMillis = Duration.ofMinutes(accessTokenExpiryMinutes).toMillis();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiryMillis)))
                .signWith(key)
                .compact();
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMillis / 1000;
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
