package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.port.out.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil implements TokenService {

    // R-13: Reject secrets below the minimum safe length at startup.
    private static final int MIN_SECRET_BYTES = 32;
    private static final String DEFAULT_DEV_SECRET =
            "hotelux-super-secret-key-change-in-production-min-32-chars";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expirationMs;

    public JwtUtil(
            @Value("${hospitops.jwt.secret}") String secret,
            @Value("${hospitops.jwt.expiration-ms:28800000}") long expirationMs
    ) {
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "JWT secret is too short: " + keyBytes.length + " bytes. " +
                            "Minimum is " + MIN_SECRET_BYTES + " bytes. " +
                            "Set the JWT_SECRET environment variable to a cryptographically " +
                            "random string of at least " + MIN_SECRET_BYTES + " characters.");
        }
        if (secret.equals(DEFAULT_DEV_SECRET)) {
            log.warn("*** SECURITY WARNING: Default JWT secret is in use. " +
                    "Set JWT_SECRET in your environment before deploying to production. ***");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expirationMs = expirationMs;
    }

    @Override
    public String generate(Staff staff) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        var builder = JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(staff.getId().value().toString())
                .withClaim("username", staff.getUsername())
                .withClaim("role", staff.getRole().name())
                .withClaim("fullName", staff.getFullName())
                .withIssuedAt(now)
                .withExpiresAt(expiry);

        // Embed hotelId so JwtAuthFilter can bind HotelContext without a DB lookup
        if (staff.getHotelId() != null) {
            builder = builder.withClaim("hotelId", staff.getHotelId().value().toString());
        }

        return builder.sign(algorithm);
    }

    /**
     * Verifies and decodes a JWT string.
     *
     * @throws JWTVerificationException if the token is invalid or expired
     */
    public DecodedJWT parse(String token) {
        return verifier.verify(token);
    }

    public boolean isValid(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
