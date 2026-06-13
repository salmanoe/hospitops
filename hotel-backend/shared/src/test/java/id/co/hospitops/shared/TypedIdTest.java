package id.co.hospitops.shared;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests for all typed ID value objects.
 *
 * <p>Every {@link DomainId} implementation must satisfy the same contract:
 * {@code generate()} produces a unique non-null UUID, {@code of(UUID)} and
 * {@code of(String)} round-trip the value correctly, and the record equality
 * is value-based.
 */
@DisplayName("Typed IDs")
class TypedIdTest {

    // ── GroupId ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GroupId")
    class GroupIdTests {

        @Test
        @DisplayName("generate() returns a non-null ID")
        void generateReturnsNonNull() {
            assertThat(GroupId.generate()).isNotNull();
            assertThat(GroupId.generate().value()).isNotNull();
        }

        @Test
        @DisplayName("generate() produces unique values")
        void generateProducesUniqueValues() {
            assertThat(GroupId.generate()).isNotEqualTo(GroupId.generate());
        }

        @Test
        @DisplayName("of(UUID) round-trips the value")
        void ofUuidRoundTrips() {
            UUID uuid = UUID.randomUUID();
            assertThat(GroupId.of(uuid).value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of(String) round-trips the value")
        void ofStringRoundTrips() {
            UUID uuid = UUID.randomUUID();
            assertThat(GroupId.of(uuid.toString()).value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of(String) rejects an invalid UUID string")
        void ofStringRejectsInvalidInput() {
            assertThatThrownBy(() -> GroupId.of("not-a-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("equality is value-based")
        void equalityIsValueBased() {
            UUID uuid = UUID.randomUUID();
            assertThat(GroupId.of(uuid)).isEqualTo(GroupId.of(uuid));
        }

        @Test
        @DisplayName("implements DomainId")
        void implementsDomainId() {
            assertThat(GroupId.generate()).isInstanceOf(DomainId.class);
        }
    }

    // ── HotelId ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("HotelId")
    class HotelIdTests {

        @Test
        @DisplayName("generate() returns a non-null ID")
        void generateReturnsNonNull() {
            assertThat(HotelId.generate()).isNotNull();
            assertThat(HotelId.generate().value()).isNotNull();
        }

        @Test
        @DisplayName("generate() produces unique values")
        void generateProducesUniqueValues() {
            assertThat(HotelId.generate()).isNotEqualTo(HotelId.generate());
        }

        @Test
        @DisplayName("of(UUID) round-trips the value")
        void ofUuidRoundTrips() {
            UUID uuid = UUID.randomUUID();
            assertThat(HotelId.of(uuid).value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of(String) round-trips the value")
        void ofStringRoundTrips() {
            UUID uuid = UUID.randomUUID();
            assertThat(HotelId.of(uuid.toString()).value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of(String) rejects an invalid UUID string")
        void ofStringRejectsInvalidInput() {
            assertThatThrownBy(() -> HotelId.of("not-a-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("equality is value-based")
        void equalityIsValueBased() {
            UUID uuid = UUID.randomUUID();
            assertThat(HotelId.of(uuid)).isEqualTo(HotelId.of(uuid));
        }

        @Test
        @DisplayName("implements DomainId")
        void implementsDomainId() {
            assertThat(HotelId.generate()).isInstanceOf(DomainId.class);
        }

        @Test
        @DisplayName("HotelId and GroupId with same UUID are not equal")
        void differentTypesWithSameUuidAreNotEqual() {
            UUID uuid = UUID.randomUUID();
            assertThat(HotelId.of(uuid)).isNotEqualTo(GroupId.of(uuid));
        }
    }
}
