package com.passwordleakdetector.dto.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HistoryEntryRequest(
        @NotBlank(message = "Site name is required")
        @Size(max = 255)
        String siteName,

        @Size(max = 500)
        String siteUrl,

        @Size(max = 255)
        String siteUsername,

        @NotBlank(message = "Password is required")
        String password,

        @Size(max = 1000)
        String notes
) {
}
