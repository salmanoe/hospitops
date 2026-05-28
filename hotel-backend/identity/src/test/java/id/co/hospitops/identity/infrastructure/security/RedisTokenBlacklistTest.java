package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisTokenBlacklist (R-02 fix).
 *
 * <p>Uses Mockito mocks for {@link StringRedisTemplate} and {@link JwtUtil} so
 * tests run without a real Redis instance. The integration concern (Redis
 * connectivity) is covered at the Docker Compose / Testcontainers layer.
 */
@DisplayName("RedisTokenBlacklist")
@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock JwtUtil jwtUtil;
    @Mock DecodedJWT decodedJwt;

    @InjectMocks RedisTokenBlacklist blacklist;

    // ── invalidate ────────────────────────────────────────────────

    @Nested
    @DisplayName("invalidate()")
    class Invalidate {

        @BeforeEach
        void wireValueOps() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("stores key with TTL derived from JWT expiry")
        void storesKeyWithRemainingTtl() {
            String token = "valid.jwt.token";
            Instant expiry = Instant.now().plusSeconds(3600);
            when(jwtUtil.parse(token)).thenReturn(decodedJwt);
            when(decodedJwt.getExpiresAtAsInstant()).thenReturn(expiry);

            blacklist.invalidate(token);

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOps).set(
                    eq(RedisTokenBlacklist.KEY_PREFIX + token),
                    eq(""),
                    ttlCaptor.capture());
            assertThat(ttlCaptor.getValue())
                    .as("TTL must be positive and at most the full expiry window")
                    .isPositive()
                    .isLessThanOrEqualTo(Duration.ofSeconds(3600));
        }

        @Test
        @DisplayName("falls back to configured expiration seconds when token cannot be parsed")
        void fallsBackToConfiguredExpirationOnParseFailure() {
            String token = "unparseable.token";
            when(jwtUtil.parse(token)).thenThrow(new JWTVerificationException("bad token"));
            when(jwtUtil.getExpirationSeconds()).thenReturn(28800L);

            blacklist.invalidate(token);

            verify(valueOps).set(
                    eq(RedisTokenBlacklist.KEY_PREFIX + token),
                    eq(""),
                    eq(Duration.ofSeconds(28800)));
        }

        @Test
        @DisplayName("uses a minimum TTL of 1 second when the token is already expired")
        void usesMinimumTtlForAlreadyExpiredToken() {
            String token = "already.expired.token";
            Instant pastExpiry = Instant.now().minusSeconds(60); // already expired
            when(jwtUtil.parse(token)).thenReturn(decodedJwt);
            when(decodedJwt.getExpiresAtAsInstant()).thenReturn(pastExpiry);

            blacklist.invalidate(token);

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOps).set(
                    eq(RedisTokenBlacklist.KEY_PREFIX + token),
                    eq(""),
                    ttlCaptor.capture());
            assertThat(ttlCaptor.getValue())
                    .as("TTL must be at least 1 second so Redis accepts the SET command")
                    .isEqualTo(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("key always starts with the expected prefix")
        void keyHasCorrectPrefix() {
            String token = "some.token";
            when(jwtUtil.parse(token)).thenReturn(decodedJwt);
            when(decodedJwt.getExpiresAtAsInstant()).thenReturn(Instant.now().plusSeconds(100));

            blacklist.invalidate(token);

            verify(valueOps).set(
                    startsWith(RedisTokenBlacklist.KEY_PREFIX),
                    any(),
                    any(Duration.class));
        }
    }

    // ── isBlacklisted ─────────────────────────────────────────────

    @Nested
    @DisplayName("isBlacklisted()")
    class IsBlacklisted {

        @Test
        @DisplayName("returns true when Redis key exists")
        void trueWhenKeyExists() {
            String token = "blacklisted.token";
            when(redisTemplate.hasKey(RedisTokenBlacklist.KEY_PREFIX + token))
                    .thenReturn(Boolean.TRUE);

            assertThat(blacklist.isBlacklisted(token)).isTrue();
        }

        @Test
        @DisplayName("returns false when Redis key does not exist")
        void falseWhenKeyAbsent() {
            String token = "fresh.token";
            when(redisTemplate.hasKey(RedisTokenBlacklist.KEY_PREFIX + token))
                    .thenReturn(Boolean.FALSE);

            assertThat(blacklist.isBlacklisted(token)).isFalse();
        }

        @Test
        @DisplayName("returns false when Redis returns null (key expired between check and read)")
        void falseWhenRedisReturnsNull() {
            String token = "race.condition.token";
            when(redisTemplate.hasKey(RedisTokenBlacklist.KEY_PREFIX + token))
                    .thenReturn(null);

            assertThat(blacklist.isBlacklisted(token)).isFalse();
        }

        @Test
        @DisplayName("checks the correct prefixed key — not the raw token")
        void checksCorrectPrefixedKey() {
            String token = "test.token";
            when(redisTemplate.hasKey(any())).thenReturn(Boolean.FALSE);

            blacklist.isBlacklisted(token);

            verify(redisTemplate).hasKey(RedisTokenBlacklist.KEY_PREFIX + token);
            verify(redisTemplate, never()).hasKey(token); // must NOT query unprefixed key
        }
    }
}
