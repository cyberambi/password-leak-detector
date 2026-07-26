package com.passwordleakdetector.dto.password;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GeneratePasswordRequest(
        @NotNull @Min(8) @Max(128) Integer length,
        boolean includeUppercase,
        boolean includeLowercase,
        boolean includeDigits,
        boolean includeSymbols
) {
}
