package com.klu.spring.mvc.common;

import java.time.Instant;

public class TokenValidationResult {

    private final boolean valid;
    private final String message;
    private final String username;
    private final String role;
    private final Instant issuedAt;
    private final Instant expiresAt;

    public TokenValidationResult(boolean valid, String message, String username, String role,
                                 Instant issuedAt, Instant expiresAt) {
        this.valid = valid;
        this.message = message;
        this.username = username;
        this.role = role;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static TokenValidationResult invalid(String message) {
        return new TokenValidationResult(false, message, null, null, null, null);
    }

    public boolean isValid() { return valid; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
