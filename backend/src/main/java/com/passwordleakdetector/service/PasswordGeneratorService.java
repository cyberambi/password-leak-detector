package com.passwordleakdetector.service;

import com.passwordleakdetector.dto.password.GeneratePasswordRequest;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordGeneratorService {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}<>?";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(GeneratePasswordRequest request) {
        List<String> selectedSets = new ArrayList<>();
        if (request.includeLowercase()) {
            selectedSets.add(LOWERCASE);
        }
        if (request.includeUppercase()) {
            selectedSets.add(UPPERCASE);
        }
        if (request.includeDigits()) {
            selectedSets.add(DIGITS);
        }
        if (request.includeSymbols()) {
            selectedSets.add(SYMBOLS);
        }
        if (selectedSets.isEmpty()) {
            throw new IllegalArgumentException("At least one character set must be selected");
        }

        int length = request.length();
        if (length < selectedSets.size()) {
            throw new IllegalArgumentException(
                    "Length must be at least " + selectedSets.size() + " to include one character from each selected set");
        }

        String combinedPool = String.join("", selectedSets);
        char[] result = new char[length];

        // Guarantee at least one character from every selected set, so a request for
        // e.g. digits + symbols can't come back with a password that happens to have
        // neither by chance.
        for (int i = 0; i < selectedSets.size(); i++) {
            result[i] = randomChar(selectedSets.get(i));
        }
        for (int i = selectedSets.size(); i < length; i++) {
            result[i] = randomChar(combinedPool);
        }

        shuffle(result);
        return new String(result);
    }

    private char randomChar(String pool) {
        return pool.charAt(secureRandom.nextInt(pool.length()));
    }

    private void shuffle(char[] chars) {
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
    }
}
