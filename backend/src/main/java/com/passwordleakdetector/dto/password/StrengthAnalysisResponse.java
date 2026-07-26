package com.passwordleakdetector.dto.password;

import java.util.List;

public record StrengthAnalysisResponse(int score, String label, List<String> feedback) {
}
