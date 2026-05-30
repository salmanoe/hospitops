package id.co.hospitops.room.application;

import id.co.hospitops.room.application.command.CreateRoomCommand;
import id.co.hospitops.room.application.response.RoomResponse;
import id.co.hospitops.room.domain.model.Room;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.room.domain.port.out.RoomRateOverrideRepository;
import id.co.hospitops.room.domain.port.out.RoomRepository;
import id.co.hospitops.room.domain.port.out.RoomTypeRepository;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ConflictException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoomService — application layer.
 * <p>
 * Covers:
 * - createRoom: duplicate-number conflict, unknown roomType, happy path
 * - findAll: valid/invalid/null status filter (R-18 fix)
 * - isAvailable: delegates to repository
 * - resolveRate: falls back to base price when no override applies
 * - markOccupied / markDirty / markAvailable: state transitions saved to repo
 * <p>
 * Room type management tests live in RoomTypeServiceTest.
 */
@DisplayName("RoomService")
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    RoomRepository roomRepo;
    @Mock
    RoomTypeRepository roomTypeRepo;
    @Mock
    RoomRateOverrideRepository overrideRepo;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    RoomService service;

    // ── helpers ─────────────────────────────────────────────────────
    private static RoomType stubRoomType() {
        return RoomType.create(HotelId.generate(), "Deluxe", 2, "Sea view", Money.of(new BigDecimal("500000")));
    }

    private static Room stubRoom(RoomTypeId rtId) {
        return Room.create(HotelId.generate(), "101", 1, rtId, null);
    }

    private final Pageable page = PageRequest.of(0, 20);

    // ── createRoom ───────────────────────────────────────────────────

    @Nested
    @DisplayName("createRoom()")
    class CreateRoom {

        @Test
        @DisplayName("throws ConflictException when room number already exists")
        void throwsOnDuplicateNumber() {
            when(roomRepo.existsByRoomNumber("101")).thenReturn(true);
            var cmd = new CreateRoomCommand("101", 1, RoomTypeId.generate(), null);

            assertThatThrownBy(() -> service.createRoom(cmd))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("101");

            verify(roomRepo, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when room type does not exist")
        void throwsWhenRoomTypeNotFound() {
            RoomTypeId rtId = RoomTypeId.generate();
            when(roomRepo.existsByRoomNumber("102")).thenReturn(false);
            when(roomTypeRepo.findById(rtId)).thenReturn(Optional.empty());

            var cmd = new CreateRoomCommand("102", 2, rtId, null);

            assertThatThrownBy(() -> service.createRoom(cmd))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("saves and returns response for valid room")
        void savesNewRoom() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            when(roomRepo.existsByRoomNumber("101")).thenReturn(false);
            when(roomTypeRepo.findById(rt.getId())).thenReturn(Optional.of(rt));
            when(roomRepo.save(any(Room.class))).thenReturn(room);

            var cmd = new CreateRoomCommand("101", 1, rt.getId(), null);

            // createRoom() calls HotelContext.current() to stamp the new Room.
            // Bind a ScopedValue here as the HotelContextInterceptor would in production.
            RoomResponse[] holder = new RoomResponse[1];
            ScopedValue.where(HotelContext.HOTEL_ID, HotelId.generate())
                    .run(() -> holder[0] = service.createRoom(cmd));

            assertThat(holder[0].roomNumber()).isEqualTo("101");
            assertThat(holder[0].floor()).isEqualTo(1);
            assertThat(holder[0].status()).isEqualTo(RoomStatus.AVAILABLE);
            verify(roomRepo).save(any());
        }
    }

    // ── findAll ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll() — status filter (R-18)")
    class FindAll {

        @Test
        @DisplayName("null statusFilter returns all rooms")
        void nullFilterReturnsAll() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            when(roomRepo.findAll(page)).thenReturn(List.of(room));
            when(roomRepo.count()).thenReturn(1L);
            when(roomTypeRepo.findById(rt.getId())).thenReturn(Optional.of(rt));

            var result = service.findAll(null, page);

            assertThat(result.content()).hasSize(1);
            verify(roomRepo).findAll(page);
            verify(roomRepo, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("blank statusFilter returns all rooms")
        void blankFilterReturnsAll() {
            when(roomRepo.findAll(page)).thenReturn(List.of());
            when(roomRepo.count()).thenReturn(0L);

            var result = service.findAll("  ", page);

            assertThat(result.content()).isEmpty();
            verify(roomRepo).findAll(page);
        }

        @ParameterizedTest(name = "status = {0}")
        @ValueSource(strings = {"AVAILABLE", "OCCUPIED", "DIRTY", "MAINTENANCE"})
        @DisplayName("valid status delegates to findByStatus()")
        void validStatusDelegatesToFindByStatus(String status) {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            when(roomRepo.findByStatus(any(), eq(page))).thenReturn(List.of(room));
            when(roomRepo.countByStatus(any())).thenReturn(1L);
            when(roomTypeRepo.findById(rt.getId())).thenReturn(Optional.of(rt));

            var result = service.findAll(status, page);

            assertThat(result.content()).hasSize(1);
            verify(roomRepo).findByStatus(RoomStatus.valueOf(status), page);
            verify(roomRepo, never()).findAll(any(Pageable.class));
        }

        @ParameterizedTest(name = "bad = \"{0}\"")
        @ValueSource(strings = {"CLEAN", "BOOKED", "free", "999"})
        @DisplayName("unknown status throws BusinessRuleViolationException")
        void unknownStatusThrows(String bad) {
            assertThatThrownBy(() -> service.findAll(bad, page))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(bad);
            verifyNoInteractions(roomRepo);
        }
    }

    // ── isAvailable ──────────────────────────────────────────────────

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailable {

        @Test
        @DisplayName("returns true when repository confirms availability")
        void returnsTrueWhenAvailable() {
            RoomId id = RoomId.generate();
            LocalDate ci = LocalDate.of(2025, 6, 1), co = LocalDate.of(2025, 6, 5);
            when(roomRepo.isAvailable(id, ci, co)).thenReturn(true);

            assertThat(service.isAvailable(id, ci, co)).isTrue();
        }

        @Test
        @DisplayName("returns false when repository reports unavailable")
        void returnsFalseWhenNotAvailable() {
            RoomId id = RoomId.generate();
            LocalDate ci = LocalDate.of(2025, 6, 1), co = LocalDate.of(2025, 6, 5);
            when(roomRepo.isAvailable(id, ci, co)).thenReturn(false);

            assertThat(service.isAvailable(id, ci, co)).isFalse();
        }
    }

    // ── resolveRate ──────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveRate()")
    class ResolveRate {

        @Test
        @DisplayName("returns base price when no rate override applies")
        void returnsBasePriceWhenNoOverride() {
            RoomType rt = stubRoomType(); // basePrice = 500_000
            Room room = stubRoom(rt.getId());
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
            when(roomTypeRepo.findById(rt.getId())).thenReturn(Optional.of(rt));
            when(overrideRepo.findByRoomTypeId(rt.getId())).thenReturn(List.of());

            Money rate = service.resolveRate(room.getId(), LocalDate.of(2025, 7, 1));

            assertThat(rate.amount()).isEqualByComparingTo("500000");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown room")
        void throwsForUnknownRoom() {
            RoomId id = RoomId.generate();
            when(roomRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolveRate(id, LocalDate.now()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── State transitions ────────────────────────────────────────────

    @Nested
    @DisplayName("status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("markOccupied() changes AVAILABLE room to OCCUPIED and saves")
        void markOccupied() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId()); // starts AVAILABLE
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

            service.markOccupied(room.getId());

            assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
            verify(roomRepo).save(room);
        }

        @Test
        @DisplayName("markDirty() changes OCCUPIED room to DIRTY and saves")
        void markDirty() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            room.markOccupied(); // AVAILABLE → OCCUPIED
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

            service.markDirty(room.getId());

            assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
            verify(roomRepo).save(room);
        }

        @Test
        @DisplayName("markAvailable() changes DIRTY room to AVAILABLE and saves")
        void markAvailable() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            room.markOccupied();
            room.markDirty(); // OCCUPIED → DIRTY
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

            service.markAvailable(room.getId());

            assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
            verify(roomRepo).save(room);
        }
    }

    // ── changeRoomStatus (housekeeping endpoint) ─────────────────────────

    @Nested
    @DisplayName("changeRoomStatus() — housekeeping endpoint")
    class ChangeRoomStatus {

        @Test
        @DisplayName("SERVICE_REQUESTED transitions OCCUPIED room and saves")
        void serviceRequested() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            room.markOccupied(); // AVAILABLE → OCCUPIED
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

            service.changeRoomStatus(room.getId(), RoomStatus.SERVICE_REQUESTED, "fresh towels");

            assertThat(room.getStatus()).isEqualTo(RoomStatus.SERVICE_REQUESTED);
            verify(roomRepo).save(room);
        }

        @Test
        @DisplayName("OCCUPIED (via changeRoomStatus) completes service and restores OCCUPIED")
        void serviceComplete() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            room.markOccupied();
            room.requestService(); // OCCUPIED → SERVICE_REQUESTED
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

            service.changeRoomStatus(room.getId(), RoomStatus.OCCUPIED, null);

            assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
            verify(roomRepo).save(room);
        }

        @Test
        @DisplayName("DIRTY is still rejected (only set via checkout event)")
        void dirtyRejected() {
            RoomType rt = stubRoomType();
            Room room = stubRoom(rt.getId());
            room.markOccupied();
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> service.changeRoomStatus(room.getId(), RoomStatus.DIRTY, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("DIRTY");

            verify(roomRepo, never()).save(any());
        }
    }
}
