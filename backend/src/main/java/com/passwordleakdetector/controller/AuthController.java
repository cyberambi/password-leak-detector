package com.passwordleakdetector.controller;

import com.passwordleakdetector.dto.auth.AuthResponse;
import com.passwordleakdetector.dto.auth.LoginRequest;
import com.passwordleakdetector.dto.auth.RegisterRequest;
import com.passwordleakdetector.dto.auth.RegisterResponse;
import com.passwordleakdetector.service.AuthResult;
import com.passwordleakdetector.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                           @Value("${app.security.cookie-secure}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result).toString())
                .body(new AuthResponse(result.accessToken(), result.accessTokenExpiresInSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        AuthResult result = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result).toString())
                .body(new AuthResponse(result.accessToken(), result.accessTokenExpiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildClearedCookie().toString())
                .build();
    }

    private ResponseCookie buildRefreshCookie(AuthResult result) {
        Duration maxAge = Duration.between(java.time.Instant.now(), result.refreshTokenExpiresAt());
        return ResponseCookie.from(REFRESH_COOKIE_NAME, result.rawRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    private ResponseCookie buildClearedCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
