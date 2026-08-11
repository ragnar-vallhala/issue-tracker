package com.its.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifies the tokens the User Service issues.
 *
 * <p>This is the only place in the system where a token's signature is checked. Services
 * behind the gateway trust the identity headers it injects, which is sound only while
 * their ports are unreachable from outside (DESIGN section 9).
 */
@Component
public class JwtValidator {

    private final SecretKey signingKey;

    public JwtValidator(@Value("${its.jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "its.jwt.secret must be at least 32 bytes for HS256; got " + keyBytes.length);
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * @return the token's claims, or empty if it is malformed, tampered with or expired.
     *         The caller gets a 401 either way - distinguishing "expired" from "forged"
     *         in the response tells an attacker which of their guesses was closer.
     */
    public Optional<Claims> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(claims);

        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
