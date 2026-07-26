package com.passwordleakdetector.service;

import com.passwordleakdetector.dto.password.StrengthAnalysisResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordStrengthServiceTest {

    private final PasswordStrengthService strengthService = new PasswordStrengthService();

    @Test
    void exactCommonPasswordScoresZero() {
        StrengthAnalysisResponse result = strengthService.analyze("password");

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.label()).isEqualTo("Very Weak");
        assertThat(result.feedback()).anyMatch(f -> f.contains("common leaked passwords"));
    }

    @Test
    void sequentialCharacterRunScoresZeroAndIsFlagged() {
        StrengthAnalysisResponse result = strengthService.analyze("abcdefgh");

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.feedback()).anyMatch(f -> f.contains("sequential"));
    }

    @Test
    void keyboardRowRunScoresZeroAndIsFlagged() {
        StrengthAnalysisResponse result = strengthService.analyze("qwertyui");

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.feedback()).anyMatch(f -> f.contains("keyboard patterns"));
    }

    @Test
    void repeatedCharacterRunScoresZeroAndIsFlagged() {
        StrengthAnalysisResponse result = strengthService.analyze("aaaaaaaa");

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.feedback()).anyMatch(f -> f.contains("repeating"));
    }

    @Test
    void longRandomPasswordWithAllCharacterClassesScoresVeryStrong() {
        StrengthAnalysisResponse result = strengthService.analyze("V9#hK2$mT7@qM4!r");

        assertThat(result.score()).isEqualTo(4);
        assertThat(result.label()).isEqualTo("Very Strong");
        assertThat(result.feedback()).anyMatch(f -> f.contains("Strong password"));
    }

    @Test
    void missingCharacterClassesAreCalledOutInFeedback() {
        StrengthAnalysisResponse result = strengthService.analyze("thisisonlylowercaseletters");

        assertThat(result.feedback())
                .contains("Add uppercase letters, digits, symbols to increase strength");
    }

    @Test
    void shortPasswordIsFlaggedForLength() {
        StrengthAnalysisResponse result = strengthService.analyze("aB3!");

        assertThat(result.feedback()).anyMatch(f -> f.contains("at least 8 characters"));
    }
}
