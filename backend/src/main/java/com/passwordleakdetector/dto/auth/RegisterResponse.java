package com.passwordleakdetector.dto.auth;

import java.time.Instant;

public record RegisterResponse(Long id, String username, Instant createdAt) {
}
