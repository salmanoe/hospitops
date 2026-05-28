package id.co.hospitops.identity.infrastructure.security;

import id.co.hospitops.identity.domain.port.out.RefreshTokenStore;
import id.co.hospitops.shared.StaffId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-node in-memory fallback for {@link RefreshTokenStore}.
 *
 * <p>Active when {@code hospitops.redis.enabled} is {@code false} or absent
 * ({@code matchIfMissing = true}). Tokens are held in a {@link ConcurrentHashMap}
 * keyed by token string, with a companion expiry instant. A scheduled task evicts
 * expired entries every 10 minutes. Tokens are lost on application restart —
 * acceptable for local development where sessions are short-lived.
 *
 * <p>In production (K8s multi-replica), set {@code hospitops.redis.enabled=true}
 * to activate {@link RedisRefreshTokenStore} instead.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hospitops.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private record Entry(StaffId staffId, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void store(String refreshToken, StaffId staffId, long ttlSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        store.put(refreshToken, new Entry(staffId, expiresAt));
        log.debug("Refresh token stored in memory for staff {}, expires {}", staffId, expiresAt);
    }

    @Override
    public Optional<StaffId> findStaffId(String refreshToken) {
        Entry entry = store.get(refreshToken);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            store.remove(refreshToken); // lazy eviction
            return Optional.empty();
        }
        return Optional.of(entry.staffId());
    }

    @Override
    public void revoke(String refreshToken) {
        store.remove(refreshToken);
        log.debug("Refresh token revoked from in-memory store");
    }

    /**
     * Periodic eviction of expired entries. Runs every 10 minutes.
     * Prevents unbounded growth in long-running dev sessions.
     */
    @Scheduled(fixedRateString = "PT10M")
    void evictExpired() {
        Instant now = Instant.now();
        int before = store.size();
        store.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        int removed = before - store.size();
        if (removed > 0) {
            log.debug("Evicted {} expired refresh tokens ({} remaining)", removed, store.size());
        }
    }
}
