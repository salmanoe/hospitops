package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * R-02 FIX: Redis-backed token blacklist for multi-replica K8s deployments.
 *
 * <p>The {@link InMemoryTokenBlacklist} is per-JVM — in a cluster of N replicas
 * a token invalidated on replica A is still accepted by replicas B and C.
 * This implementation stores invalidated tokens in Redis so every replica
 * shares the same blacklist. Redis key TTL mirrors the JWT expiration,
 * so entries are automatically purged without any scheduled task.
 *
 * <p>Activated when {@code hospitops.redis.enabled=true}. When the property
 * is absent or false the {@link InMemoryTokenBlacklist} (annotated with
 * {@code @ConditionalOnMissingBean}) is used instead — transparent fallback
 * for local development and tests that have no Redis available.
 *
 * <h3>Redis key schema</h3>
 * {@code blacklist:<token>} → empty string, TTL = JWT remaining lifetime.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hospitops.redis.enabled", havingValue = "true")
public class RedisTokenBlacklist implements TokenBlacklist {

    static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    public void invalidate(String token) {
        Duration ttl = resolveTtl(token);
        redisTemplate.opsForValue().set(KEY_PREFIX + token, "", ttl);
        log.debug("Token blacklisted in Redis with TTL {}", ttl);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }

    /**
     * Calculates the remaining lifetime of the token so Redis can expire the
     * key automatically. Falls back to the configured JWT expiration seconds
     * when the token cannot be parsed (already expired / malformed — safe to
     * use a short TTL since it cannot be reused anyway).
     */
    private Duration resolveTtl(String token) {
        try {
            Instant expiry = jwtUtil.parse(token).getExpiresAtAsInstant();
            Duration remaining = Duration.between(Instant.now(), expiry);
            // Guard against negative TTL (token already expired): use 1s minimum
            // so Redis accepts the SET command and the key expires immediately.
            return remaining.isNegative() ? Duration.ofSeconds(1) : remaining;
        } catch (JWTVerificationException | IllegalArgumentException e) {
            log.debug("Could not parse expiry for blacklist TTL: {}", e.getMessage());
            return Duration.ofSeconds(jwtUtil.getExpirationSeconds());
        }
    }
}
