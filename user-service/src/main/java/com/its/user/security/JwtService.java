package com.its.user.security;

import com.its.user.entity.Role;
import com.its.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues the signed tokens that authenticate every subsequent request (SRS A-05).
 *
 * <p>This service <em>issues</em> tokens; it does not verify them. Verification happens
 * once, at the gateway, which then injects {@code X-User-Id} and {@code X-User-Role} for
 * downstream services to trust (DESIGN section 9). Both sides must be configured with
 * the same {@code JWT_SECRET}.
 *
 * <p>The secret has a development default so the system starts on a clean machine, but
 * it is an environment variable in every other case - a signing key in version control
 * is a signing key that is public.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirySeconds;

    public JwtService(
            @Value("${its.jwt.secret}") String secret,
            @Value("${its.jwt.expiry-seconds}") long expirySeconds) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // HS256 requires at least 256 bits of key material. jjwt enforces this, but the
        // failure surfaces at first login rather than at start-up, which is a poor place
        // to discover a misconfiguration.
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "its.jwt.secret must be at least 32 bytes for HS256; got " + keyBytes.length);
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirySeconds = expirySeconds;
    }

    /**
     * Mints a token for a freshly authenticated user.
     *
     * <p>The subject is the user id and the single custom claim is the role <em>name</em>,
     * never the stored 0/1 digit - the encoding stays behind the persistence boundary
     * (SRS A-04), and a token carrying a bare integer would be far easier to misread at
     * the point where it actually gates access.
     */
    public String issue(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirySeconds);

        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    /** Exposed for tests asserting the claim contents. */
    public Role roleOf(String token) {
        String role = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
        return Role.valueOf(role);
    }
}
