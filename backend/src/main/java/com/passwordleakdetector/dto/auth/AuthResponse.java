package com.passwordleakdetector.dto.auth;

public record AuthResponse(String accessToken, long expiresIn) {
}
