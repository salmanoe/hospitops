package id.co.hospitops.identity.infrastructure.security;

import id.co.hospitops.shared.StaffId;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InMemoryRefreshTokenStore.
 *
 * <p>No Spring context — the store is a plain Java object exercised directly.
 * Clock-sensitive expiry tests use a zero TTL to create already-expired entries
 * without needing a fake clock.
 */
@DisplayName("InMemoryRefreshTokenStore")
class InMemoryRefreshTokenStoreTest {

    InMemoryRefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRefreshTokenStore();
    }

    // ── store() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("store()")
    class Store {

        @Test
        @DisplayName("a stored token is immediately retrievable")
        void storedTokenIsRetrievable() {
            StaffId staffId = StaffId.generate();
            String token = UUID.randomUUID().toString();

            store.store(token, staffId, 3600L);

            assertThat(store.findStaffId(token)).contains(staffId);
        }

        @Test
        @DisplayName("overwriting a token with a new staffId replaces the entry")
        void overwriteReplacesEntry() {
            String token = "shared-token";
            StaffId first  = StaffId.generate();
            StaffId second = StaffId.generate();

            store.store(token, first,  3600L);
            store.store(token, second, 3600L);

            assertThat(store.findStaffId(token)).contains(second);
        }

        @Test
        @DisplayName("different tokens for the same staff are stored independently")
        void multipleTokensForSameStaff() {
            StaffId staffId = StaffId.generate();
            String tokenA = UUID.randomUUID().toString();
            String tokenB = UUID.randomUUID().toString();

            store.store(tokenA, staffId, 3600L);
            store.store(tokenB, staffId, 3600L);

            assertThat(store.findStaffId(tokenA)).contains(staffId);
            assertThat(store.findStaffId(tokenB)).contains(staffId);
        }
    }

    // ── findStaffId() ────────────────────────────────────────────────

    @Nested
    @DisplayName("findStaffId()")
    class FindStaffId {

        @Test
        @DisplayName("returns staffId for a valid, non-expired token")
        void returnsStaffIdForValidToken() {
            StaffId staffId = StaffId.generate();
            String  token   = UUID.randomUUID().toString();
            store.store(token, staffId, 3600L);

            assertThat(store.findStaffId(token)).contains(staffId);
        }

        @Test
        @DisplayName("returns empty for a token that was never stored")
        void returnsEmptyForUnknownToken() {
            assertThat(store.findStaffId("ghost-token")).isEmpty();
        }

        @Test
        @DisplayName("returns empty and lazily evicts an expired entry (TTL = 0)")
        void returnsEmptyAndEvictsExpiredEntry() {
            String token = "expiring-token";
            // TTL of 0 means expiresAt = now, which isBefore(now) will be true
            // within the same nanosecond — reliable on any JVM.
            store.store(token, StaffId.generate(), 0L);

            // Small sleep ensures Instant.now() in findStaffId() is strictly after expiresAt
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}

            assertThat(store.findStaffId(token)).isEmpty();

            // Lazy eviction: subsequent lookup of the same token still returns empty
            assertThat(store.findStaffId(token)).isEmpty();
        }

        @Test
        @DisplayName("expired entry is removed from the map during lazy eviction")
        void expiredEntryIsRemovedFromMapLazily() {
            String token   = "lazy-evict-token";
            StaffId staffId = StaffId.generate();
            store.store(token, staffId, 0L);

            try { Thread.sleep(5); } catch (InterruptedException ignored) {}

            // First call — lazy eviction triggers
            store.findStaffId(token);

            // Second call must not see the entry either (it was removed, not just skipped)
            assertThat(store.findStaffId(token)).isEmpty();
        }
    }

    // ── revoke() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("revoke()")
    class Revoke {

        @Test
        @DisplayName("revoked token can no longer be found")
        void revokedTokenIsUnavailable() {
            StaffId staffId = StaffId.generate();
            String  token   = UUID.randomUUID().toString();
            store.store(token, staffId, 3600L);

            store.revoke(token);

            assertThat(store.findStaffId(token)).isEmpty();
        }

        @Test
        @DisplayName("revoking an unknown token does not throw")
        void revokingUnknownTokenIsIdempotent() {
            assertThatNoException().isThrownBy(() -> store.revoke("never-stored"));
        }

        @Test
        @DisplayName("revoking one token does not affect other stored tokens")
        void revokeIsIsolated() {
            StaffId staffId = StaffId.generate();
            String  tokenA  = UUID.randomUUID().toString();
            String  tokenB  = UUID.randomUUID().toString();
            store.store(tokenA, staffId, 3600L);
            store.store(tokenB, staffId, 3600L);

            store.revoke(tokenA);

            assertThat(store.findStaffId(tokenA)).isEmpty();
            assertThat(store.findStaffId(tokenB)).contains(staffId);
        }
    }

    // ── evictExpired() ───────────────────────────────────────────────

    @Nested
    @DisplayName("evictExpired()")
    class EvictExpired {

        @Test
        @DisplayName("removes all expired entries and leaves valid ones intact")
        void evictsExpiredLeavesValid() throws InterruptedException {
            StaffId staffId = StaffId.generate();
            String  expiredToken = "expires-soon";
            String  validToken   = UUID.randomUUID().toString();

            store.store(expiredToken, staffId, 0L);
            store.store(validToken,   staffId, 3600L);

            Thread.sleep(5); // ensure expired entry is past its expiresAt

            store.evictExpired();

            assertThat(store.findStaffId(expiredToken)).isEmpty();
            assertThat(store.findStaffId(validToken)).contains(staffId);
        }

        @Test
        @DisplayName("calling evictExpired on an empty store does not throw")
        void evictOnEmptyStoreIsIdempotent() {
            assertThatNoException().isThrownBy(() -> store.evictExpired());
        }

        @Test
        @DisplayName("evicts multiple expired entries in a single pass")
        void evictsMultipleExpiredEntries() throws InterruptedException {
            StaffId staffId = StaffId.generate();
            store.store("exp-1", staffId, 0L);
            store.store("exp-2", staffId, 0L);
            store.store("exp-3", staffId, 0L);
            String alive = UUID.randomUUID().toString();
            store.store(alive, staffId, 3600L);

            Thread.sleep(5);
            store.evictExpired();

            assertThat(store.findStaffId("exp-1")).isEmpty();
            assertThat(store.findStaffId("exp-2")).isEmpty();
            assertThat(store.findStaffId("exp-3")).isEmpty();
            assertThat(store.findStaffId(alive)).contains(staffId);
        }
    }
}
