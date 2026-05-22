package id.co.hospitops.identity.domain.port.out;

import id.co.hospitops.identity.domain.model.Staff;

/**
 * Port — token generation and validation.
 *
 * <p>Defined in the domain/port/out layer so application services can depend on
 * this interface without importing any infrastructure class (JwtUtil, JJWT, etc.).
 * The infrastructure implementation is {@code JwtUtil}.
 */
public interface TokenService {

    /**
     * Generate a signed token for the given staff member.
     */
    String generate(Staff staff);

    /**
     * Return the token lifetime in seconds (used in the login response so
     * the client knows when to refresh).
     */
    long getExpirationSeconds();

    /**
     * Return the raw token string embedded in a Bearer Authorization header,
     * or {@code null} / throw if the header is malformed.
     */
    default String extractFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
