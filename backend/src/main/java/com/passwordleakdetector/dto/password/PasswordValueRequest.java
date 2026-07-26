package com.passwordleakdetector.dto.password;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared request shape for endpoints that accept a single password value to
 * evaluate (check-breach, analyze-strength). The password is never logged or
 * persisted by either endpoint.
 */
public record PasswordValueRequest(
        @NotBlank(message = "Password is required")
        String password
) {
}
