package com.passwordleakdetector.dto.password;

public record BreachCheckResponse(boolean breached, long occurrences) {
}
