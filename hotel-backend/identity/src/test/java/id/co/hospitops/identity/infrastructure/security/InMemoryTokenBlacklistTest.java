package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for InMemoryTokenBlacklist.
 *
 * Covers:
 *   - isBlacklisted() returns false for a never-seen token
 *   - After invalidate(), isBlacklisted() returns true
 *   - invalidate() gracefully falls back when JWT parsing fails
 *   - evictExpiredTokens() removes entries whose expiry has passed
 *   - evictExpiredTokens() retains entries that have not yet expired
 */
@DisplayName("InMemoryTokenBlacklist")
@ExtendWith(MockitoExtension.class)
class InMemoryTokenBlacklistTest {

    @Mock JwtUtil jwtUtil;
    @Mock DecodedJWT decodedJWT;

    InMemoryTokenBlacklist blacklist;

    @BeforeEach
    void setUp() {
        blacklist = new InMemoryTokenBlacklist(jwtUtil);
    }

    // ── isBlacklisted: initial state ─────────────────────────────────────

    @Nested
    @DisplayName("isBlacklisted() — before any invalidation")
    class InitialState {

        @Test
        @DisplayName("returns false for a token that was never invalidated")
        void falseForUnknownToken() {
            assertThat(blacklist.isBlacklisted("never-seen-token")).isFalse();
        }

        @Test
        @DisplayName("returns false for an empty string")
        void falseForEmptyString() {
            assertThat(blacklist.isBlacklisted("")).isFalse();
        }
    }

    // ── invalidate + isBlacklisted ────────────────────────────────────────

    @Nested
    @DisplayName("invalidate() + isBlacklisted()")
    class InvalidateAndCheck {

        @Test
        @DisplayName("after invalidation, isBlacklisted() returns true")
        void isBlacklistedAfterInvalidation() {
            given(jwtUtil.parse("token-abc")).willReturn(decodedJWT);
            given(decodedJWT.getExpiresAtAsInstant()).willReturn(Instant.now().plusSeconds(3600));

            blacklist.invalidate("token-abc");

            assertThat(blacklist.isBlacklisted("token-abc")).isTrue();
        }

        @Test
        @DisplayName("different tokens are tracked independently")
        void differentTokensTrackedIndependently() {
            given(jwtUtil.parse("token-1")).willReturn(decodedJWT);
            given(decodedJWT.getExpiresAtAsInstant())
                    .willReturn(Instant.now().plusSeconds(3600));

            blacklist.invalidate("token-1");
            // "token-2" was never invalidated, so no stub needed for its parse

            assertThat(blacklist.isBlacklisted("token-1")).isTrue();
            assertThat(blacklist.isBlacklisted("token-2")).isFalse();
        }

        @Test
        @DisplayName("invalidating the same token twice does not throw")
        void invalidateSameTokenTwiceIsIdempotent() {
            given(jwtUtil.parse(any())).willReturn(decodedJWT);
            given(decodedJWT.getExpiresAtAsInstant())
                    .willReturn(Instant.now().plusSeconds(3600));

            assertThatNoException().isThrownBy(() -> {
                blacklist.invalidate("dup-token");
                blacklist.invalidate("dup-token");
            });
            assertThat(blacklist.isBlacklisted("dup-token")).isTrue();
        }
    }

    // ── fallback when JWT parse fails ────────────────────────────────────

    @Nested
    @DisplayName("invalidate() — parse failure fallback")
    class ParseFailureFallback {

        @Test
        @DisplayName("falls back to getExpirationSeconds() when token cannot be parsed")
        void fallsBackOnJwtVerificationException() {
            given(jwtUtil.parse("malformed-token"))
                    .willThrow(new JWTVerificationException("bad token"));
            given(jwtUtil.getExpirationSeconds()).willReturn(3600L);

            assertThatNoException().isThrownBy(
                    () -> blacklist.invalidate("malformed-token"));

            assertThat(blacklist.isBlacklisted("malformed-token")).isTrue();
        }

        @Test
        @DisplayName("falls back on IllegalArgumentException from parse")
        void fallsBackOnIllegalArgumentException() {
            given(jwtUtil.parse("bad-token"))
                    .willThrow(new IllegalArgumentException("null input"));
            given(jwtUtil.getExpirationSeconds()).willReturn(1800L);

            assertThatNoException().isThrownBy(
                    () -> blacklist.invalidate("bad-token"));

            assertThat(blacklist.isBlacklisted("bad-token")).isTrue();
        }
    }

    // ── evictExpiredTokens ────────────────────────────────────────────────

    @Nested
    @DisplayName("evictExpiredTokens()")
    class EvictExpiredTokens {

        @Test
        @DisplayName("removes tokens whose expiry is in the past")
        void removesExpiredTokens() {
            given(jwtUtil.parse("expired-token")).willReturn(decodedJWT);
            given(decodedJWT.getExpiresAtAsInstant())
                    .willReturn(Instant.now().minusSeconds(1)); // already expired

            blacklist.invalidate("expired-token");
            blacklist.evictExpiredTokens();

            assertThat(blacklist.isBlacklisted("expired-token")).isFalse();
        }

        @Test
        @DisplayName("retains tokens whose expiry is still in the future")
        void retainsNonExpiredTokens() {
            given(jwtUtil.parse("valid-token")).willReturn(decodedJWT);
            given(decodedJWT.getExpiresAtAsInstant())
                    .willReturn(Instant.now().plusSeconds(7200));

            blacklist.invalidate("valid-token");
            blacklist.evictExpiredTokens();

            assertThat(blacklist.isBlacklisted("valid-token")).isTrue();
        }

        @Test
        @DisplayName("eviction is a no-op on an empty blacklist")
        void noOpOnEmptyBlacklist() {
            assertThatNoException().isThrownBy(() -> blacklist.evictExpiredTokens());
        }

        @Test
        @DisplayName("evicts only expired; retains non-expired")
        void evictsOnlyExpiredTokens() {
            DecodedJWT expiredDecoded  = mock(DecodedJWT.class);
            DecodedJWT freshDecoded    = mock(DecodedJWT.class);

            given(jwtUtil.parse("expired")).willReturn(expiredDecoded);
            given(jwtUtil.parse("fresh")).willReturn(freshDecoded);
            given(expiredDecoded.getExpiresAtAsInstant())
                    .willReturn(Instant.now().minusSeconds(60));
            given(freshDecoded.getExpiresAtAsInstant())
                    .willReturn(Instant.now().plusSeconds(7200));

            blacklist.invalidate("expired");
            blacklist.invalidate("fresh");
            blacklist.evictExpiredTokens();

            assertThat(blacklist.isBlacklisted("expired")).isFalse();
            assertThat(blacklist.isBlacklisted("fresh")).isTrue();
        }
    }
}
