package com.prateek.ProjectExpenseManagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and parses JWTs used for stateless authentication.
 * The signing key must be at least 256 bits (32 chars) for HS256.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String INSECURE_DEFAULT_SECRET = "change-this-dev-only-secret-key-please";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes:60}") long expirationMinutes) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            // Fail fast at startup rather than silently signing tokens with a
            // weak key - a short HS256 key is brute-forceable.
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters. " +
                    "Set the JWT_SECRET environment variable to a strong random value.");
        }
        if (INSECURE_DEFAULT_SECRET.equals(secret)) {
            log.warn("jwt.secret is set to the built-in development default. " +
                    "Set the JWT_SECRET environment variable before deploying to production.");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMinutes * 60 * 1000;
    }

    public String generateToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
