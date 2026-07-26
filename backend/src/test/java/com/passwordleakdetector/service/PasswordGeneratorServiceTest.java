package com.passwordleakdetector.service;

import com.passwordleakdetector.dto.password.GeneratePasswordRequest;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordGeneratorServiceTest {

    private final PasswordGeneratorService generatorService = new PasswordGeneratorService();

    @Test
    void generatesPasswordOfRequestedLength() {
        String password = generatorService.generate(new GeneratePasswordRequest(20, true, true, true, true));

        assertThat(password).hasSize(20);
    }

    @Test
    void respectsSelectedCharacterSets() {
        // digits + symbols only, no letters
        String password = generatorService.generate(new GeneratePasswordRequest(16, false, false, true, true));

        assertThat(password).matches("[0-9!@#$%^&*()\\-_=+\\[\\]{}<>?]+");
        assertThat(password).containsPattern("[0-9]");
        assertThat(password).containsPattern("[!@#$%^&*()\\-_=+\\[\\]{}<>?]");
    }

    @Test
    void guaranteesAtLeastOneCharacterFromEachSelectedSet() {
        for (int i = 0; i < 50; i++) {
            String password = generatorService.generate(new GeneratePasswordRequest(8, true, true, true, true));

            assertThat(password).containsPattern("[A-Z]");
            assertThat(password).containsPattern("[a-z]");
            assertThat(password).containsPattern("[0-9]");
            assertThat(password).containsPattern("[^A-Za-z0-9]");
        }
    }

    @Test
    void rejectsRequestWithNoCharacterSetSelected() {
        assertThatThrownBy(() -> generatorService.generate(new GeneratePasswordRequest(16, false, false, false, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLengthShorterThanNumberOfSelectedSets() {
        assertThatThrownBy(() -> generatorService.generate(new GeneratePasswordRequest(2, true, true, true, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotProduceIdenticalPasswordsAcrossCalls() {
        boolean allIdentical = IntStream.range(0, 20)
                .mapToObj(i -> generatorService.generate(new GeneratePasswordRequest(16, true, true, true, true)))
                .distinct()
                .count() == 1;

        assertThat(allIdentical).isFalse();
    }
}
