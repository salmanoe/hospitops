package id.co.hospitops.reservation.infrastructure.persistence;

import id.co.hospitops.reservation.domain.model.Reservation;
import id.co.hospitops.reservation.domain.model.ReservationStatus;
import id.co.hospitops.reservation.infrastructure.persistence.entity.ReservationJpaEntity;
import id.co.hospitops.shared.*;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ReservationMapper (MapStruct @Mapper interface).
 *
 * Uses Mappers.getMapper() for Spring-componentModel mappers in tests.
 * Covers toJpa(), toDomain(), round-trip, and null createdBy handling.
 */
@DisplayName("ReservationMapper")
class ReservationMapperTest {

    private final ReservationMapper mapper = Mappers.getMapper(ReservationMapper.class);

    private static final LocalDate     CHECK_IN   = LocalDate.of(2025, 7, 10);
    private static final LocalDate     CHECK_OUT  = LocalDate.of(2025, 7, 13);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2025, 7, 1, 10, 0);

    private Reservation buildReservation(StaffId createdBy) {
        return Reservation.reconstitute(
                ReservationId.generate(), "RES-2025-00001",
                GuestId.generate(), RoomId.generate(),
                CHECK_IN, CHECK_OUT,
                ReservationStatus.CONFIRMED, Money.of(800_000L),
                2, 1, "Late check-in requested",
                createdBy, CREATED_AT, CREATED_AT);
    }

    // ── toJpa ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toJpa()")
    class ToJpa {

        @Test
        @DisplayName("maps reservation UUID correctly")
        void mapsId() {
            Reservation r = buildReservation(StaffId.generate());
            assertThat(mapper.toJpa(r).getId()).isEqualTo(r.getId().value());
        }

        @Test
        @DisplayName("maps reservationNumber correctly")
        void mapsReservationNumber() {
            Reservation r = buildReservation(StaffId.generate());
            assertThat(mapper.toJpa(r).getReservationNumber()).isEqualTo("RES-2025-00001");
        }

        @Test
        @DisplayName("maps guestId as raw UUID")
        void mapsGuestId() {
            Reservation r = buildReservation(StaffId.generate());
            assertThat(mapper.toJpa(r).getGuestId()).isEqualTo(r.getGuestId().value());
        }

        @Test
        @DisplayName("maps roomId as raw UUID")
        void mapsRoomId() {
            Reservation r = buildReservation(StaffId.generate());
            assertThat(mapper.toJpa(r).getRoomId()).isEqualTo(r.getRoomId().value());
        }

        @Test
        @DisplayName("maps status correctly")
        void mapsStatus() {
            Reservation r = buildReservation(StaffId.generate());
            assertThat(mapper.toJpa(r).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("maps ratePerNight as BigDecimal")
        void mapsRatePerNight() {
            Reservation r = buildReservation(StaffId.generate());
            assertThat(mapper.toJpa(r).getRatePerNight())
                    .isEqualByComparingTo(BigDecimal.valueOf(800_000));
        }

        @Test
        @DisplayName("maps adults and children correctly")
        void mapsGuestCounts() {
            Reservation r = buildReservation(StaffId.generate());
            ReservationJpaEntity entity = mapper.toJpa(r);
            assertThat(entity.getAdults()).isEqualTo(2);
            assertThat(entity.getChildren()).isEqualTo(1);
        }

        @Test
        @DisplayName("maps createdBy as raw UUID when present")
        void mapsCreatedBy() {
            StaffId createdBy = StaffId.generate();
            Reservation r = buildReservation(createdBy);
            assertThat(mapper.toJpa(r).getCreatedBy()).isEqualTo(createdBy.value());
        }

        @Test
        @DisplayName("maps createdBy as null when absent")
        void mapsNullCreatedBy() {
            Reservation r = buildReservation(null);
            assertThat(mapper.toJpa(r).getCreatedBy()).isNull();
        }

        @Test
        @DisplayName("maps specialRequests correctly")
        void mapsSpecialRequests() {
            Reservation r = buildReservation(StaffId.generate());
            assertThat(mapper.toJpa(r).getSpecialRequests()).isEqualTo("Late check-in requested");
        }
    }

    // ── toDomain ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("maps all fields from entity to domain object")
        void mapsAllFields() {
            UUID guestId = UUID.randomUUID();
            UUID roomId  = UUID.randomUUID();
            UUID staffId = UUID.randomUUID();

            ReservationJpaEntity entity = ReservationJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .reservationNumber("RES-2025-00099")
                    .guestId(guestId)
                    .roomId(roomId)
                    .createdBy(staffId)
                    .checkInDate(CHECK_IN)
                    .checkOutDate(CHECK_OUT)
                    .status(ReservationStatus.CONFIRMED)
                    .ratePerNight(BigDecimal.valueOf(750_000))
                    .adults(2)
                    .children(0)
                    .specialRequests("High floor please")
                    .createdAt(CREATED_AT)
                    .updatedAt(CREATED_AT)
                    .build();

            Reservation domain = mapper.toDomain(entity);

            assertThat(domain.getReservationNumber()).isEqualTo("RES-2025-00099");
            assertThat(domain.getGuestId().value()).isEqualTo(guestId);
            assertThat(domain.getRoomId().value()).isEqualTo(roomId);
            assertThat(domain.getCreatedBy().value()).isEqualTo(staffId);
            assertThat(domain.getCheckInDate()).isEqualTo(CHECK_IN);
            assertThat(domain.getCheckOutDate()).isEqualTo(CHECK_OUT);
            assertThat(domain.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(domain.getRatePerNight().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(750_000));
            assertThat(domain.getAdults()).isEqualTo(2);
            assertThat(domain.getChildren()).isEqualTo(0);
            assertThat(domain.getSpecialRequests()).isEqualTo("High floor please");
        }

        @Test
        @DisplayName("maps null createdBy without NPE")
        void mapsNullCreatedBy() {
            ReservationJpaEntity entity = ReservationJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .reservationNumber("RES-X")
                    .guestId(UUID.randomUUID())
                    .roomId(UUID.randomUUID())
                    .createdBy(null)
                    .checkInDate(CHECK_IN)
                    .checkOutDate(CHECK_OUT)
                    .status(ReservationStatus.CONFIRMED)
                    .ratePerNight(BigDecimal.valueOf(500_000))
                    .adults(1)
                    .children(0)
                    .specialRequests(null)
                    .createdAt(CREATED_AT)
                    .updatedAt(CREATED_AT)
                    .build();

            Reservation domain = mapper.toDomain(entity);

            assertThat(domain.getCreatedBy()).isNull();
        }
    }

    // ── Round-trip ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip: Reservation -> toJpa() -> toDomain()")
    class RoundTrip {

        @Test
        @DisplayName("all fields survive a full round-trip")
        void allFieldsSurviveRoundTrip() {
            StaffId     staffId = StaffId.generate();
            Reservation original = buildReservation(staffId);

            Reservation restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.getId().value()).isEqualTo(original.getId().value());
            assertThat(restored.getReservationNumber()).isEqualTo(original.getReservationNumber());
            assertThat(restored.getGuestId()).isEqualTo(original.getGuestId());
            assertThat(restored.getRoomId()).isEqualTo(original.getRoomId());
            assertThat(restored.getStatus()).isEqualTo(original.getStatus());
            assertThat(restored.getRatePerNight().amount())
                    .isEqualByComparingTo(original.getRatePerNight().amount());
            assertThat(restored.getAdults()).isEqualTo(original.getAdults());
            assertThat(restored.getChildren()).isEqualTo(original.getChildren());
            assertThat(restored.getCreatedBy().value()).isEqualTo(original.getCreatedBy().value());
        }

        @Test
        @DisplayName("round-trip preserves null createdBy")
        void roundTripPreservesNullCreatedBy() {
            Reservation original = buildReservation(null);
            Reservation restored = mapper.toDomain(mapper.toJpa(original));
            assertThat(restored.getCreatedBy()).isNull();
        }
    }
}
