package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil secret validation and token lifecycle.
 *
 * <p>Covers the R-13 fail-fast check: the constructor must reject secrets
 * that are shorter than 32 bytes, preventing weak keys from silently
 * reaching production.
 */
@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String VALID_SECRET =
            "this-is-a-cryptographically-safe-secret-32-chars-min!!";
    private static final String SHORT_SECRET = "tooshort";
    private static final String DEFAULT_SECRET =
            "hotelux-super-secret-key-change-in-production-min-32-chars";
    private static final long EXPIRY_MS = 3_600_000L; // 1 hour

    // ── Constructor validation (R-13) ────────────────────────────────────

    @Test
    @DisplayName("short secret (< 32 bytes) throws IllegalArgumentException")
    void shortSecretThrows() {
        assertThatThrownBy(() -> new JwtUtil(SHORT_SECRET, EXPIRY_MS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too short")
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("exactly 32-byte secret constructs without error")
    void exactlyMinLengthSecretSucceeds() {
        assertThatNoException().isThrownBy(() -> new JwtUtil("a".repeat(32), EXPIRY_MS));
    }

    @Test
    @DisplayName("secret longer than 32 bytes constructs without error")
    void longSecretSucceeds() {
        assertThatNoException().isThrownBy(() -> new JwtUtil(VALID_SECRET, EXPIRY_MS));
    }

    @Test
    @DisplayName("default dev secret constructs (logs warning, does not throw)")
    void defaultSecretConstructsWithWarning() {
        // The default secret is >= 32 bytes so construction must succeed;
        // a WARN log is emitted but we don't assert on log output here.
        assertThatNoException().isThrownBy(() -> new JwtUtil(DEFAULT_SECRET, EXPIRY_MS));
    }

    // ── Token round-trip ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Token generation and parsing")
    class TokenRoundTrip {

        private JwtUtil jwt;
        private Staff staff;

        @BeforeEach
        void setUp() {
            jwt = new JwtUtil(VALID_SECRET, EXPIRY_MS);
            staff = Staff.create("Alice Manager", "alice", "hash", StaffRole.MANAGER);
        }

        @Test
        @DisplayName("generated token is parseable by the same util")
        void tokenIsParseable() {
            String token = jwt.generate(staff);
            assertThatNoException().isThrownBy(() -> jwt.parse(token));
        }

        @Test
        @DisplayName("parsed claims contain correct subject (staff UUID)")
        void claimsContainSubject() {
            String token = jwt.generate(staff);
            DecodedJWT decoded = jwt.parse(token);
            assertThat(decoded.getSubject())
                    .isEqualTo(staff.getId().value().toString());
        }

        @Test
        @DisplayName("parsed claims contain username and role")
        void claimsContainUsernameAndRole() {
            String token = jwt.generate(staff);
            DecodedJWT decoded = jwt.parse(token);
            assertThat(decoded.getClaim("username").asString()).isEqualTo("alice");
            assertThat(decoded.getClaim("role").asString()).isEqualTo("MANAGER");
        }

        @Test
        @DisplayName("isValid returns true for a freshly generated token")
        void isValidTrueForFreshToken() {
            assertThat(jwt.isValid(jwt.generate(staff))).isTrue();
        }

        @Test
        @DisplayName("isValid returns false for a garbage string")
        void isValidFalseForGarbage() {
            assertThat(jwt.isValid("not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("token signed with a different secret fails validation")
        void tokenFromDifferentSecretFails() {
            JwtUtil other = new JwtUtil("another-totally-different-secret-value-!!", EXPIRY_MS);
            String foreignToken = other.generate(staff);
            assertThat(jwt.isValid(foreignToken)).isFalse();
        }
    }
}
