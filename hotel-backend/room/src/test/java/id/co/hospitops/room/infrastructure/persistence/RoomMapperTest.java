package id.co.hospitops.room.infrastructure.persistence;

import id.co.hospitops.room.domain.model.*;
import id.co.hospitops.room.infrastructure.persistence.entity.*;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.*;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RoomMapper (MapStruct @Mapper interface).
 * <p>
 * Covers three mapping pairs independently:
 * - RoomType  <-> RoomTypeJpaEntity
 * - Room      <-> RoomJpaEntity
 * - RoomRateOverride <-> RoomRateOverrideJpaEntity
 * <p>
 * Each section exercises toJpa(), toDomain(), and a round-trip.
 */
@DisplayName("RoomMapper")
class RoomMapperTest {

    private final RoomMapper mapper = Mappers.getMapper(RoomMapper.class);

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 8, 1, 8, 0);

    // ══════════════════════════════════════════════════════════════
    // RoomType
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RoomType mapping")
    class RoomTypeMapping {

        private RoomType sampleRoomType() {
            return RoomType.reconstitute(
                    RoomTypeId.generate(), "Deluxe", 2,
                    "Sea-view room", Money.of(750_000L), HotelId.generate(), NOW, NOW);
        }

        @Test
        @DisplayName("toJpa() maps id as raw UUID")
        void toJpaMapsId() {
            RoomType rt = sampleRoomType();
            assertThat(mapper.toJpa(rt).getId()).isEqualTo(rt.getId().value());
        }

        @Test
        @DisplayName("toJpa() maps name, capacity, description")
        void toJpaMapsStringFields() {
            RoomType rt = sampleRoomType();
            RoomTypeJpaEntity e = mapper.toJpa(rt);
            assertThat(e.getName()).isEqualTo("Deluxe");
            assertThat(e.getCapacity()).isEqualTo(2);
            assertThat(e.getDescription()).isEqualTo("Sea-view room");
        }

        @Test
        @DisplayName("toJpa() maps basePrice as BigDecimal")
        void toJpaMapsBasePrice() {
            RoomType rt = sampleRoomType();
            assertThat(mapper.toJpa(rt).getBasePrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(750_000));
        }

        @Test
        @DisplayName("toDomain() maps all fields from entity")
        void toDomainMapsAllFields() {
            UUID id = UUID.randomUUID();
            RoomTypeJpaEntity entity = RoomTypeJpaEntity.builder()
                    .id(id).name("Suite").capacity(4)
                    .description("Presidential suite")
                    .basePrice(BigDecimal.valueOf(2_000_000))
                    .createdAt(NOW).updatedAt(NOW)
                    .hotelId(java.util.UUID.randomUUID())
                    .build();

            RoomType rt = mapper.toDomain(entity);

            assertThat(rt.getId().value()).isEqualTo(id);
            assertThat(rt.getName()).isEqualTo("Suite");
            assertThat(rt.getCapacity()).isEqualTo(4);
            assertThat(rt.getDescription()).isEqualTo("Presidential suite");
            assertThat(rt.getBasePrice().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(2_000_000));
        }

        @Test
        @DisplayName("round-trip preserves all fields")
        void roundTripPreservesAllFields() {
            RoomType original = sampleRoomType();
            RoomType restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.getId().value()).isEqualTo(original.getId().value());
            assertThat(restored.getName()).isEqualTo(original.getName());
            assertThat(restored.getCapacity()).isEqualTo(original.getCapacity());
            assertThat(restored.getDescription()).isEqualTo(original.getDescription());
            assertThat(restored.getBasePrice().amount())
                    .isEqualByComparingTo(original.getBasePrice().amount());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Room
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Room mapping")
    class RoomMapping {

        private Room sampleRoom() {
            return Room.reconstitute(
                    RoomId.generate(), "101", 1,
                    RoomStatus.AVAILABLE, RoomTypeId.generate(),
                    null, HotelId.generate(), NOW, NOW);
        }

        @Test
        @DisplayName("toJpa() maps id as raw UUID")
        void toJpaMapsId() {
            Room room = sampleRoom();
            assertThat(mapper.toJpa(room).getId()).isEqualTo(room.getId().value());
        }

        @Test
        @DisplayName("toJpa() maps roomNumber and floor")
        void toJpaMapsRoomNumberAndFloor() {
            Room room = sampleRoom();
            RoomJpaEntity e = mapper.toJpa(room);
            assertThat(e.getRoomNumber()).isEqualTo("101");
            assertThat(e.getFloor()).isEqualTo(1);
        }

        @Test
        @DisplayName("toJpa() maps status correctly")
        void toJpaMapsStatus() {
            Room room = sampleRoom();
            assertThat(mapper.toJpa(room).getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        }

        @Test
        @DisplayName("toJpa() maps roomTypeId as raw UUID")
        void toJpaMapsRoomTypeId() {
            Room room = sampleRoom();
            assertThat(mapper.toJpa(room).getRoomTypeId()).isEqualTo(room.getRoomTypeId().value());
        }

        @Test
        @DisplayName("toJpa() maps null notes correctly")
        void toJpaMapsNullNotes() {
            Room room = sampleRoom();
            assertThat(mapper.toJpa(room).getNotes()).isNull();
        }

        @Test
        @DisplayName("toJpa() maps non-null notes correctly")
        void toJpaMapsNonNullNotes() {
            Room room = Room.reconstitute(RoomId.generate(), "202", 2,
                    RoomStatus.MAINTENANCE, RoomTypeId.generate(),
                    "AC repair", HotelId.generate(), NOW, NOW);
            assertThat(mapper.toJpa(room).getNotes()).isEqualTo("AC repair");
        }

        @Test
        @DisplayName("toDomain() maps all fields from entity")
        void toDomainMapsAllFields() {
            UUID id = UUID.randomUUID();
            UUID roomTypeId = UUID.randomUUID();

            RoomJpaEntity entity = RoomJpaEntity.builder()
                    .id(id).roomNumber("305").floor(3)
                    .status(RoomStatus.DIRTY)
                    .roomTypeId(roomTypeId).notes("stained carpet")
                    .createdAt(NOW).updatedAt(NOW)
                    .hotelId(java.util.UUID.randomUUID())
                    .build();

            Room room = mapper.toDomain(entity);

            assertThat(room.getId().value()).isEqualTo(id);
            assertThat(room.getRoomNumber()).isEqualTo("305");
            assertThat(room.getFloor()).isEqualTo(3);
            assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
            assertThat(room.getRoomTypeId().value()).isEqualTo(roomTypeId);
            assertThat(room.getNotes()).isEqualTo("stained carpet");
        }

        @Test
        @DisplayName("round-trip preserves all fields including status")
        void roundTripPreservesFields() {
            Room original = Room.reconstitute(RoomId.generate(), "101", 1,
                    RoomStatus.OCCUPIED, RoomTypeId.generate(), "VIP guest", HotelId.generate(), NOW, NOW);

            Room restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.getId().value()).isEqualTo(original.getId().value());
            assertThat(restored.getRoomNumber()).isEqualTo(original.getRoomNumber());
            assertThat(restored.getFloor()).isEqualTo(original.getFloor());
            assertThat(restored.getStatus()).isEqualTo(original.getStatus());
            assertThat(restored.getNotes()).isEqualTo(original.getNotes());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // RoomRateOverride
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RoomRateOverride mapping")
    class RoomRateOverrideMapping {

        private static final LocalDate FROM = LocalDate.of(2025, 12, 20);
        private static final LocalDate UNTIL = LocalDate.of(2026, 1, 3);

        private RoomRateOverride sampleOverride() {
            return new RoomRateOverride(UUID.randomUUID(), RoomTypeId.generate(),
                    "Holiday Rate", Money.of(1_200_000L), FROM, UNTIL);
        }

        @Test
        @DisplayName("toJpa() maps all fields correctly")
        void toJpaMapsAllFields() {
            RoomRateOverride override = sampleOverride();
            RoomRateOverrideJpaEntity entity = mapper.toJpa(override);

            assertThat(entity.getId()).isEqualTo(override.id());
            assertThat(entity.getRoomTypeId()).isEqualTo(override.roomTypeId().value());
            assertThat(entity.getName()).isEqualTo("Holiday Rate");
            assertThat(entity.getPriceOverride())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_200_000));
            assertThat(entity.getValidFrom()).isEqualTo(FROM);
            assertThat(entity.getValidUntil()).isEqualTo(UNTIL);
        }

        @Test
        @DisplayName("toDomain() maps all fields from entity")
        void toDomainMapsAllFields() {
            UUID id = UUID.randomUUID();
            UUID roomTypeId = UUID.randomUUID();

            RoomRateOverrideJpaEntity entity = RoomRateOverrideJpaEntity.builder()
                    .id(id).roomTypeId(roomTypeId)
                    .name("New Year Rate")
                    .priceOverride(BigDecimal.valueOf(1_500_000))
                    .validFrom(FROM).validUntil(UNTIL)
                    .build();

            RoomRateOverride override = mapper.toDomain(entity);

            assertThat(override.id()).isEqualTo(id);
            assertThat(override.roomTypeId().value()).isEqualTo(roomTypeId);
            assertThat(override.name()).isEqualTo("New Year Rate");
            assertThat(override.priceOverride().amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_500_000));
            assertThat(override.validFrom()).isEqualTo(FROM);
            assertThat(override.validUntil()).isEqualTo(UNTIL);
        }

        @Test
        @DisplayName("round-trip preserves all fields")
        void roundTripPreservesAllFields() {
            RoomRateOverride original = sampleOverride();
            RoomRateOverride restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.id()).isEqualTo(original.id());
            assertThat(restored.roomTypeId().value()).isEqualTo(original.roomTypeId().value());
            assertThat(restored.name()).isEqualTo(original.name());
            assertThat(restored.priceOverride().amount())
                    .isEqualByComparingTo(original.priceOverride().amount());
            assertThat(restored.validFrom()).isEqualTo(original.validFrom());
            assertThat(restored.validUntil()).isEqualTo(original.validUntil());
        }
    }
}
