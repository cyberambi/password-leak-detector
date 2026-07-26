package com.passwordleakdetector.service;

import com.passwordleakdetector.dto.password.BreachCheckResponse;
import com.passwordleakdetector.exception.BreachCheckUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Checks a password against the HIBP Pwned Passwords range API using the
 * k-anonymity model: only the first 5 hex characters of the password's SHA-1
 * hash ever leave this service. The full password and full hash are never
 * transmitted or logged.
 */
@Service
public class BreachCheckService {

    private final RestClient hibpRestClient;

    public BreachCheckService(RestClient hibpRestClient) {
        this.hibpRestClient = hibpRestClient;
    }

    public BreachCheckResponse check(String password) {
        String sha1Hex = sha1Hex(password);
        String prefix = sha1Hex.substring(0, 5);
        String suffix = sha1Hex.substring(5);

        String responseBody;
        try {
            responseBody = hibpRestClient.get()
                    .uri(prefix)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new BreachCheckUnavailableException(
                    "Breach check service is temporarily unavailable, please try again later", e);
        }

        if (responseBody == null) {
            return new BreachCheckResponse(false, 0);
        }

        for (String line : responseBody.split("\r?\n")) {
            String[] parts = line.split(":");
            if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                return new BreachCheckResponse(true, Long.parseLong(parts[1].trim()));
            }
        }
        return new BreachCheckResponse(false, 0);
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm unavailable", e);
        }
    }
}
