package com.passwordleakdetector.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username may only contain letters, digits, '.', '_' and '-'")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be at least 8 characters")
        String password
) {
}
