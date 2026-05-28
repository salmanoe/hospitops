package id.co.hospitops.identity.infrastructure.security;

import id.co.hospitops.shared.StaffId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisRefreshTokenStore.
 *
 * <p>Uses Mockito mocks for {@link StringRedisTemplate} — no real Redis needed.
 */
@DisplayName("RedisRefreshTokenStore")
@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks RedisRefreshTokenStore store;

    // ── store ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("store()")
    class Store {

        @BeforeEach
        void wireValueOps() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("writes staffId string with correct key prefix and TTL")
        void writesWithCorrectKeyAndTtl() {
            StaffId staffId      = StaffId.generate();
            String  refreshToken = UUID.randomUUID().toString();
            long    ttl          = 604_800L;

            store.store(refreshToken, staffId, ttl);

            verify(valueOps).set(
                    eq(RedisRefreshTokenStore.KEY_PREFIX + refreshToken),
                    eq(staffId.value().toString()),
                    eq(Duration.ofSeconds(ttl)));
        }

        @Test
        @DisplayName("key always starts with the expected prefix")
        void keyHasCorrectPrefix() {
            store.store("any-token", StaffId.generate(), 3600L);

            verify(valueOps).set(
                    startsWith(RedisRefreshTokenStore.KEY_PREFIX),
                    any(),
                    any(Duration.class));
        }
    }

    // ── findStaffId ───────────────────────────────────────────────

    @Nested
    @DisplayName("findStaffId()")
    class FindStaffId {

        @BeforeEach
        void wireValueOps() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("returns staffId when key exists in Redis")
        void returnsStaffIdWhenPresent() {
            StaffId staffId      = StaffId.generate();
            String  refreshToken = UUID.randomUUID().toString();
            when(valueOps.get(RedisRefreshTokenStore.KEY_PREFIX + refreshToken))
                    .thenReturn(staffId.value().toString());

            Optional<StaffId> result = store.findStaffId(refreshToken);

            assertThat(result).contains(staffId);
        }

        @Test
        @DisplayName("returns empty when key is absent (token expired or never stored)")
        void returnsEmptyWhenAbsent() {
            when(valueOps.get(any())).thenReturn(null);

            assertThat(store.findStaffId("missing-token")).isEmpty();
        }

        @Test
        @DisplayName("returns empty when Redis value is malformed (not a valid UUID)")
        void returnsEmptyForMalformedValue() {
            when(valueOps.get(any())).thenReturn("not-a-uuid");

            assertThat(store.findStaffId("token")).isEmpty();
        }

        @Test
        @DisplayName("queries the correct prefixed key")
        void queriesCorrectKey() {
            String token = "my-refresh-token";
            when(valueOps.get(any())).thenReturn(null);

            store.findStaffId(token);

            verify(valueOps).get(RedisRefreshTokenStore.KEY_PREFIX + token);
        }
    }

    // ── revoke ────────────────────────────────────────────────────

    @Nested
    @DisplayName("revoke()")
    class Revoke {

        @Test
        @DisplayName("deletes the correct prefixed key")
        void deletesCorrectKey() {
            String token = "token-to-revoke";

            store.revoke(token);

            verify(redisTemplate).delete(RedisRefreshTokenStore.KEY_PREFIX + token);
        }

        @Test
        @DisplayName("succeeds silently when key does not exist")
        void succeedsWhenKeyAbsent() {
            when(redisTemplate.delete(anyString())).thenReturn(Boolean.FALSE);

            assertThatNoException().isThrownBy(() -> store.revoke("ghost-token"));
        }
    }
}
