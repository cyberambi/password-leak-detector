package com.passwordleakdetector.service;

import com.passwordleakdetector.dto.password.StrengthAnalysisResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Custom entropy + pattern-penalty password scorer (chosen over pulling in the
 * zxcvbn4j dependency so scoring stays small, auditable and precisely
 * unit-testable - see project plan for the rationale).
 */
@Service
public class PasswordStrengthService {

    private static final List<String> KEYBOARD_ROWS = List.of(
            "qwertyuiop", "asdfghjkl", "zxcvbnm", "1234567890"
    );
    private static final int MIN_DICTIONARY_WORD_LENGTH = 4;

    private final Set<String> commonPasswords;
    private final List<String> dictionaryWords;

    public PasswordStrengthService() {
        this.commonPasswords = loadLines("/strength/common-passwords.txt");
        this.dictionaryWords = new ArrayList<>(loadLines("/strength/common-words.txt"));
    }

    public StrengthAnalysisResponse analyze(String password) {
        String lower = password.toLowerCase(Locale.ROOT);
        List<String> feedback = new ArrayList<>();

        if (commonPasswords.contains(lower)) {
            feedback.add("This is one of the most common leaked passwords - choose something unique");
            return new StrengthAnalysisResponse(0, labelFor(0), feedback);
        }

        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        int poolSize = (hasLower ? 26 : 0) + (hasUpper ? 26 : 0) + (hasDigit ? 10 : 0) + (hasSymbol ? 32 : 0);
        double entropy = poolSize == 0 ? 0 : password.length() * (Math.log(poolSize) / Math.log(2));

        int sequentialPenalty = sequentialRunPenalty(lower);
        if (sequentialPenalty > 0) {
            feedback.add("Avoid sequential characters like 'abc' or '123'");
        }

        int keyboardPenalty = keyboardRunPenalty(lower);
        if (keyboardPenalty > 0) {
            feedback.add("Avoid keyboard patterns like 'qwerty' or 'asdf'");
        }

        int repeatedPenalty = repeatedRunPenalty(lower);
        if (repeatedPenalty > 0) {
            feedback.add("Avoid repeating the same character multiple times in a row");
        }

        int dictionaryPenalty = 0;
        for (String word : dictionaryWords) {
            if (word.length() >= MIN_DICTIONARY_WORD_LENGTH && lower.contains(word)) {
                dictionaryPenalty = 10;
                feedback.add("Avoid common dictionary words like '" + word + "'");
                break;
            }
        }

        double adjustedEntropy = Math.max(
                0, entropy - sequentialPenalty - keyboardPenalty - repeatedPenalty - dictionaryPenalty);

        if (password.length() < 8) {
            feedback.add("Use at least 8 characters, ideally 12 or more");
        }

        List<String> missingClasses = new ArrayList<>();
        if (!hasUpper) missingClasses.add("uppercase letters");
        if (!hasLower) missingClasses.add("lowercase letters");
        if (!hasDigit) missingClasses.add("digits");
        if (!hasSymbol) missingClasses.add("symbols");
        if (!missingClasses.isEmpty()) {
            feedback.add("Add " + String.join(", ", missingClasses) + " to increase strength");
        }

        int score = scoreFromEntropy(adjustedEntropy);
        if (feedback.isEmpty() && score >= 3) {
            feedback.add("Strong password - no immediate improvements needed");
        }

        return new StrengthAnalysisResponse(score, labelFor(score), feedback);
    }

    private int scoreFromEntropy(double entropy) {
        if (entropy < 28) return 0;
        if (entropy < 36) return 1;
        if (entropy < 60) return 2;
        if (entropy < 90) return 3;
        return 4;
    }

    private String labelFor(int score) {
        return switch (score) {
            case 0 -> "Very Weak";
            case 1 -> "Weak";
            case 2 -> "Fair";
            case 3 -> "Strong";
            default -> "Very Strong";
        };
    }

    /** Penalizes runs of 3+ ascending or descending consecutive characters, e.g. "abc", "321". */
    private int sequentialRunPenalty(String lower) {
        int penalty = 0;
        int i = 0;
        while (i < lower.length() - 2) {
            char a = lower.charAt(i);
            char b = lower.charAt(i + 1);
            char c = lower.charAt(i + 2);
            boolean ascending = (b == a + 1) && (c == b + 1);
            boolean descending = (b == a - 1) && (c == b - 1);
            if (ascending || descending) {
                int runLength = 3;
                while (i + runLength < lower.length()) {
                    char prev = lower.charAt(i + runLength - 1);
                    char next = lower.charAt(i + runLength);
                    if ((ascending && next == prev + 1) || (descending && next == prev - 1)) {
                        runLength++;
                    } else {
                        break;
                    }
                }
                penalty += runLength * 4;
                i += runLength;
            } else {
                i++;
            }
        }
        return penalty;
    }

    /** Penalizes the longest keyboard-row walk found (forward or reversed), e.g. "qwerty", "asdf". */
    private int keyboardRunPenalty(String lower) {
        int penalty = 0;
        for (String row : KEYBOARD_ROWS) {
            String reversed = new StringBuilder(row).reverse().toString();
            for (int len = Math.min(row.length(), 10); len >= 3; len--) {
                boolean matched = false;
                for (int start = 0; start + len <= row.length(); start++) {
                    String forwardSegment = row.substring(start, start + len);
                    String reverseSegment = reversed.substring(start, start + len);
                    if (lower.contains(forwardSegment) || lower.contains(reverseSegment)) {
                        penalty += len * 4;
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    break;
                }
            }
        }
        return penalty;
    }

    /** Penalizes runs of 3+ identical repeated characters, e.g. "aaa", "111". */
    private int repeatedRunPenalty(String lower) {
        int penalty = 0;
        int i = 0;
        while (i < lower.length()) {
            int runLength = 1;
            while (i + runLength < lower.length() && lower.charAt(i + runLength) == lower.charAt(i)) {
                runLength++;
            }
            if (runLength >= 3) {
                penalty += runLength * 4;
            }
            i += runLength;
        }
        return penalty;
    }

    private Set<String> loadLines(String classpathLocation) {
        Set<String> lines = new LinkedHashSet<>();
        try (InputStream is = new ClassPathResource(classpathLocation).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load wordlist: " + classpathLocation, e);
        }
        return lines;
    }
}
