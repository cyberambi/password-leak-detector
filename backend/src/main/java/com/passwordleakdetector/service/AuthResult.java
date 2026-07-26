package com.passwordleakdetector.service;

import java.time.Instant;

/**
 * Internal result of a login/refresh operation: the bearer access token plus
 * the raw (unhashed) refresh token that the controller layer sets as an
 * httpOnly cookie. The raw refresh token is never persisted - only its hash is.
 */
public record AuthResult(String accessToken, long accessTokenExpiresInSeconds,
                          String rawRefreshToken, Instant refreshTokenExpiresAt) {
}
