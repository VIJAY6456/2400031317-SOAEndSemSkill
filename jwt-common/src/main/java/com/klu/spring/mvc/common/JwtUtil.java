package com.klu.spring.mvc.common;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

/**
 * Creates and validates JWTs. Every service and the gateway share the same secret,
 * so a token issued by auth-service can be validated anywhere.
 */
public class JwtUtil {

    private final Key key;
    private final long expirationMs;

    public JwtUtil(String secret, long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Token validator: accepts the raw token or the full "Bearer <token>" header value. */
    public TokenValidationResult validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return TokenValidationResult.invalid("Token is missing");
        }

        String jwt = token.trim();
        if (jwt.regionMatches(true, 0, "Bearer ", 0, 7)) {
            jwt = jwt.substring(7).trim();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            return new TokenValidationResult(
                    true,
                    "Token is valid",
                    claims.getSubject(),
                    claims.get("role", String.class),
                    claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant(),
                    claims.getExpiration() == null ? null : claims.getExpiration().toInstant());

        } catch (ExpiredJwtException e) {
            return TokenValidationResult.invalid("Token has expired");
        } catch (MalformedJwtException e) {
            return TokenValidationResult.invalid("Malformed token");
        } catch (UnsupportedJwtException e) {
            return TokenValidationResult.invalid("Unsupported token");
        } catch (JwtException e) {
            if (e.getClass().getSimpleName().contains("Signature")) {
                return TokenValidationResult.invalid("Invalid token signature");
            }
            return TokenValidationResult.invalid("Invalid token");
        } catch (IllegalArgumentException e) {
            return TokenValidationResult.invalid("Invalid token");
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
