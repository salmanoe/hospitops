package id.co.hospitops.guest.infrastructure.persistence;

import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.guest.infrastructure.persistence.entity.GuestJpaEntity;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.HotelId;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GuestMapper (R-17 fix).
 * <p>
 * GuestMapper was extracted from GuestRepositoryImpl so that the conversion
 * logic can be tested in isolation without any Spring Data or database wiring.
 * <p>
 * Covers:
 * - Round-trip: Guest -> toJpa() -> toDomain() preserves all fields
 * - Null optional fields (idNumber, nationality, phone, email, address)
 * - toJpa() field-by-field correctness
 * - toDomain() field-by-field correctness including timestamps
 */
@DisplayName("GuestMapper")
class GuestMapperTest {

    private final GuestMapper mapper = new GuestMapper();

    // ── toJpa ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toJpa()")
    class ToJpa {

        @Test
        @DisplayName("maps all fields from Guest to GuestJpaEntity")
        void mapsAllFields() {
            Guest guest = Guest.create(HotelId.generate(),
                    "Budi Santoso", "3201010101800001",
                    "Indonesian", "+628111000001",
                    "budi@example.com", "Jl. Merdeka No. 1, Jakarta");

            GuestJpaEntity entity = mapper.toJpa(guest);

            assertThat(entity.getId()).isEqualTo(guest.getId().value());
            assertThat(entity.getFullName()).isEqualTo("Budi Santoso");
            assertThat(entity.getIdNumber()).isEqualTo("3201010101800001");
            assertThat(entity.getNationality()).isEqualTo("Indonesian");
            assertThat(entity.getPhone()).isEqualTo("+628111000001");
            assertThat(entity.getEmail()).isEqualTo("budi@example.com");
            assertThat(entity.getAddress()).isEqualTo("Jl. Merdeka No. 1, Jakarta");
        }

        @Test
        @DisplayName("maps null optional fields without NPE")
        void mapsNullOptionalFields() {
            Guest guest = Guest.create(HotelId.generate(), "Anonim", null, null, null, null, null);

            GuestJpaEntity entity = mapper.toJpa(guest);

            assertThat(entity.getIdNumber()).isNull();
            assertThat(entity.getNationality()).isNull();
            assertThat(entity.getPhone()).isNull();
            assertThat(entity.getEmail()).isNull();
            assertThat(entity.getAddress()).isNull();
        }
    }

    // ── toDomain ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("maps all fields from GuestJpaEntity to Guest")
        void mapsAllFields() {
            UUID id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            GuestJpaEntity entity = GuestJpaEntity.builder()
                    .id(id)
                    .fullName("Siti Rahayu")
                    .idNumber("3201020203900002")
                    .nationality("Indonesian")
                    .phone("+628222000002")
                    .email("siti@example.com")
                    .address("Jl. Sudirman No. 5, Bandung")
                    .createdAt(now)
                    .updatedAt(now)
                    .hotelId(java.util.UUID.randomUUID())
                    .build();

            Guest guest = mapper.toDomain(entity);

            assertThat(guest.getId().value()).isEqualTo(id);
            assertThat(guest.getFullName()).isEqualTo("Siti Rahayu");
            assertThat(guest.getIdNumber()).isEqualTo("3201020203900002");
            assertThat(guest.getNationality()).isEqualTo("Indonesian");
            assertThat(guest.getPhone()).isEqualTo("+628222000002");
            assertThat(guest.getEmail()).isEqualTo("siti@example.com");
            assertThat(guest.getAddress()).isEqualTo("Jl. Sudirman No. 5, Bandung");
            assertThat(guest.getCreatedAt()).isEqualTo(now);
            assertThat(guest.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("maps null optional fields without NPE")
        void mapsNullOptionalFields() {
            GuestJpaEntity entity = GuestJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .fullName("Anonim")
                    .idNumber(null)
                    .nationality(null)
                    .phone(null)
                    .email(null)
                    .address(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .hotelId(java.util.UUID.randomUUID())
                    .build();

            Guest guest = mapper.toDomain(entity);

            assertThat(guest.getIdNumber()).isNull();
            assertThat(guest.getNationality()).isNull();
            assertThat(guest.getPhone()).isNull();
            assertThat(guest.getEmail()).isNull();
            assertThat(guest.getAddress()).isNull();
        }
    }

    // ── Round-trip ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip: Guest -> toJpa() -> toDomain()")
    class RoundTrip {

        @Test
        @DisplayName("all fields survive a full round-trip")
        void allFieldsSurviveRoundTrip() {
            Guest original = Guest.create(HotelId.generate(),
                    "Dewi Lestari", "3201030304750003",
                    "Indonesian", "+628333000003",
                    "dewi@example.com", "Jl. Gatot Subroto No. 10, Jakarta");

            Guest restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getFullName()).isEqualTo(original.getFullName());
            assertThat(restored.getIdNumber()).isEqualTo(original.getIdNumber());
            assertThat(restored.getNationality()).isEqualTo(original.getNationality());
            assertThat(restored.getPhone()).isEqualTo(original.getPhone());
            assertThat(restored.getEmail()).isEqualTo(original.getEmail());
            assertThat(restored.getAddress()).isEqualTo(original.getAddress());
        }

        @Test
        @DisplayName("round-trip preserves null optional fields")
        void roundTripPreservesNulls() {
            Guest original = Guest.create(HotelId.generate(), "Anonim", null, null, null, null, null);

            Guest restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getFullName()).isEqualTo("Anonim");
            assertThat(restored.getIdNumber()).isNull();
            assertThat(restored.getNationality()).isNull();
            assertThat(restored.getPhone()).isNull();
            assertThat(restored.getEmail()).isNull();
            assertThat(restored.getAddress()).isNull();
        }

        @Test
        @DisplayName("round-trip with a known GuestId preserves identity")
        void roundTripWithKnownId() {
            GuestId knownId = GuestId.generate();
            LocalDateTime ts = LocalDateTime.of(2025, 1, 15, 10, 0);

            Guest original = Guest.reconstitute(
                    knownId, "Raka Wijaya", "3201040405800004",
                    "Indonesian", "+628444000004",
                    "raka@example.com", "Jl. Thamrin No. 20, Jakarta",
                    HotelId.generate(), ts, ts);

            Guest restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.getId().value()).isEqualTo(knownId.value());
            assertThat(restored.getFullName()).isEqualTo("Raka Wijaya");
            assertThat(restored.getCreatedAt()).isEqualTo(ts);
            assertThat(restored.getUpdatedAt()).isEqualTo(ts);
        }
    }
}