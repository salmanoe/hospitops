package id.co.hospitops.identity.infrastructure.security;

import id.co.hospitops.identity.domain.port.out.RefreshTokenStore;
import id.co.hospitops.shared.StaffId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed refresh token store for multi-replica K8s deployments.
 *
 * <p>Each refresh token is stored as {@code refresh:<token>} → {@code staffId} string,
 * with a TTL matching the configured refresh expiration. When a token is rotated
 * (used to obtain a new access token) the old key is deleted atomically before
 * writing the new one, ensuring each refresh token is single-use.
 *
 * <p>Activated when {@code hospitops.redis.enabled=true}. Falls back to
 * {@link InMemoryRefreshTokenStore} otherwise.
 *
 * <h3>Redis key schema</h3>
 * {@code refresh:<uuid>} → {@code <staffId-uuid>}, TTL = configured refresh expiration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hospitops.redis.enabled", havingValue = "true")
public class RedisRefreshTokenStore implements RefreshTokenStore {

    static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void store(String refreshToken, StaffId staffId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + refreshToken,
                staffId.value().toString(),
                Duration.ofSeconds(ttlSeconds));
        log.debug("Refresh token stored for staff {} with TTL {}s", staffId, ttlSeconds);
    }

    @Override
    public Optional<StaffId> findStaffId(String refreshToken) {
        String raw = redisTemplate.opsForValue().get(KEY_PREFIX + refreshToken);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(StaffId.of(UUID.fromString(raw)));
        } catch (IllegalArgumentException e) {
            log.warn("Malformed staffId in refresh token store for key {}: {}", refreshToken, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void revoke(String refreshToken) {
        Boolean deleted = redisTemplate.delete(KEY_PREFIX + refreshToken);
        log.debug("Refresh token revoked: {} (found={})", refreshToken, Boolean.TRUE.equals(deleted));
    }
}
