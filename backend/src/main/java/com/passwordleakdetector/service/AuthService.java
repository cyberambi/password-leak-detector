package com.passwordleakdetector.service;

import com.passwordleakdetector.dto.auth.LoginRequest;
import com.passwordleakdetector.dto.auth.RegisterRequest;
import com.passwordleakdetector.dto.auth.RegisterResponse;
import com.passwordleakdetector.entity.RefreshToken;
import com.passwordleakdetector.entity.User;
import com.passwordleakdetector.exception.DuplicateResourceException;
import com.passwordleakdetector.exception.InvalidCredentialsException;
import com.passwordleakdetector.exception.InvalidTokenException;
import com.passwordleakdetector.repository.RefreshTokenRepository;
import com.passwordleakdetector.repository.UserRepository;
import com.passwordleakdetector.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenExpiryDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        @Value("${app.security.refresh-token-expiry-days}") long refreshTokenExpiryDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        return new RegisterResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        String accessToken = jwtService.generateAccessToken(user);
        IssuedRefreshToken refresh = issueRefreshToken(user, UUID.randomUUID().toString());
        return new AuthResult(accessToken, jwtService.getAccessTokenExpirySeconds(),
                refresh.rawToken(), refresh.expiresAt());
    }

    // noRollbackFor is essential here: when reuse is detected we deliberately persist
    // the family-wide revocation (see below) and THEN throw to report the error -
    // without this, Spring's default rollback-on-RuntimeException would undo the very
    // revocation that is supposed to shut down the compromised token family.
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Missing refresh token");
        }
        String hash = hashToken(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (existing.isRevoked()) {
            // A revoked token was presented again: the family is compromised (stolen
            // token replayed after legitimate rotation), so kill every token in it.
            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            throw new InvalidTokenException("Refresh token reuse detected; session revoked");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = existing.getUser();
        IssuedRefreshToken next = issueRefreshToken(user, existing.getFamilyId());

        existing.setRevoked(true);
        existing.setReplacedById(next.entity().getId());

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResult(accessToken, jwtService.getAccessTokenExpirySeconds(),
                next.rawToken(), next.expiresAt());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId()));
    }

    private IssuedRefreshToken issueRefreshToken(User user, String familyId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hashToken(rawToken));
        entity.setFamilyId(familyId);
        entity.setIssuedAt(Instant.now());
        Instant expiresAt = Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS);
        entity.setExpiresAt(expiresAt);
        entity.setRevoked(false);
        entity = refreshTokenRepository.save(entity);

        return new IssuedRefreshToken(entity, rawToken, expiresAt);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private record IssuedRefreshToken(RefreshToken entity, String rawToken, Instant expiresAt) {
    }
}
