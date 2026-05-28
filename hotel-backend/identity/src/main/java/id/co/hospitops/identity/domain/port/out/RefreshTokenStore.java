package id.co.hospitops.identity.domain.port.out;

import id.co.hospitops.shared.StaffId;

import java.util.Optional;

/**
 * Port — durable storage for opaque refresh tokens.
 *
 * <p>Refresh tokens are UUID strings (not JWTs). They are persisted here with a
 * TTL equal to the configured refresh expiration. Each token maps to a single
 * {@link StaffId}; one staff member may hold multiple active refresh tokens
 * (multiple devices / sessions).
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code RedisRefreshTokenStore} — production; shared across all replicas.
 *       Active when {@code hospitops.redis.enabled=true}.</li>
 *   <li>{@code InMemoryRefreshTokenStore} — local development fallback; per-JVM,
 *       tokens lost on restart. Active when Redis is not enabled.</li>
 * </ul>
 */
public interface RefreshTokenStore {

    /**
     * Persists a new refresh token associated with the given staff member.
     *
     * @param refreshToken the opaque token string to store
     * @param staffId      the owner of this refresh session
     * @param ttlSeconds   how long before the token should be considered expired
     */
    void store(String refreshToken, StaffId staffId, long ttlSeconds);

    /**
     * Looks up the staff member who owns this refresh token.
     *
     * @param refreshToken the opaque token string
     * @return the owning {@link StaffId}, or empty if the token is unknown or expired
     */
    Optional<StaffId> findStaffId(String refreshToken);

    /**
     * Invalidates a refresh token, preventing its future use.
     * Silently succeeds if the token is already absent or expired.
     *
     * @param refreshToken the opaque token string to revoke
     */
    void revoke(String refreshToken);
}
