package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * W-5 FIX: In-memory token blacklist with expiry-based automatic eviction.
 *
 * <p>Previous implementation used an unbounded {@code Set<String>} that grew
 * forever. This version stores each invalidated token alongside its JWT
 * expiration timestamp and evicts entries once the token has naturally expired
 * (they can no longer be used anyway).
 *
 * <h3>Production note</h3>
 * This implementation is intentionally single-node. For a clustered deployment
 * use the {@link RedisTokenBlacklist} by setting {@code hospitops.redis.enabled=true}.
 * This bean is active when {@code hospitops.redis.enabled} is {@code false} or absent.
 * {@link RedisTokenBlacklist} takes over when the property is set to {@code true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hospitops.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenBlacklist implements TokenBlacklist {

    /** token → UTC instant at which the JWT itself expires */
    private final Map<String, Instant> blacklisted = new ConcurrentHashMap<>();

    private final JwtUtil jwtUtil;

    @Override
    public void invalidate(String token) {
        Instant expiry = resolveExpiry(token);
        blacklisted.put(token, expiry);
        log.debug("Token invalidated; will be evicted after {}", expiry);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return blacklisted.containsKey(token);
    }

    /**
     * Scheduled cleanup: runs every 5 minutes and removes entries whose JWT
     * expiration has already passed — those tokens are unusable regardless.
     */
    @Scheduled(fixedRateString = "PT5M")
    void evictExpiredTokens() {
        Instant now = Instant.now();
        int before = blacklisted.size();
        blacklisted.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        int removed = before - blacklisted.size();
        if (removed > 0) {
            log.debug("Evicted {} expired tokens from blacklist ({} remaining)",
                    removed, blacklisted.size());
        }
    }

    /**
     * Attempts to read the JWT expiration claim. Falls back to a 24-hour
     * horizon if the token cannot be parsed (e.g. it is already expired or
     * malformed — in both cases it cannot be reused, so a short TTL is fine).
     */
    private Instant resolveExpiry(String token) {
        try {
            return jwtUtil.parse(token).getExpiresAtAsInstant();
        } catch (JWTVerificationException | IllegalArgumentException e) {
            log.debug("Could not parse expiry from token during invalidation: {}", e.getMessage());
            return Instant.now().plusSeconds(jwtUtil.getExpirationSeconds());
        }
    }
}
