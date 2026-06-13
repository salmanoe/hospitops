package id.co.hospitops.room.application;

import id.co.hospitops.room.application.command.AddRateOverrideCommand;
import id.co.hospitops.room.application.command.CreateRoomTypeCommand;
import id.co.hospitops.room.application.command.UpdateRoomTypeCommand;
import id.co.hospitops.room.application.response.RoomTypeResponse;
import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.room.domain.port.out.RoomRateOverrideRepository;
import id.co.hospitops.room.domain.port.out.RoomTypeRepository;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.exception.ConflictException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoomTypeService — application layer.
 * <p>
 * Covers:
 * - createRoomType: duplicate-name conflict, happy path
 * - updateRoomType: not-found, happy path
 * - findRoomTypeById: not-found, happy path
 * - findAllRoomTypes: empty and non-empty pages
 * - addRateOverride: not-found, happy path
 */
@DisplayName("RoomTypeService")
@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTest {

    @Mock
    RoomTypeRepository roomTypeRepo;
    @Mock
    RoomRateOverrideRepository overrideRepo;
    @Mock
    org.springframework.context.ApplicationEventPublisher events;

    @InjectMocks
    RoomTypeService service;

    private static RoomType stubRoomType() {
        return RoomType.create(HotelId.generate(), "Deluxe", 2, "Sea view", Money.of(new BigDecimal("500000")));
    }

    private final Pageable page = PageRequest.of(0, 20);

    // ── createRoomType ───────────────────────────────────────────────

    @Nested
    @DisplayName("createRoomType()")
    class CreateRoomType {

        @Test
        @DisplayName("throws ConflictException when name already exists")
        void throwsOnDuplicateName() {
            when(roomTypeRepo.existsByName("Deluxe")).thenReturn(true);
            var cmd = new CreateRoomTypeCommand("Deluxe", 2, "desc", BigDecimal.valueOf(500000));

            assertThatThrownBy(() -> service.createRoomType(cmd))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Deluxe");

            verify(roomTypeRepo, never()).save(any());
        }

        @Test
        @DisplayName("saves and returns response for new room type")
        void savesNewRoomType() {
            when(roomTypeRepo.existsByName("Suite")).thenReturn(false);
            RoomType saved = RoomType.create(HotelId.generate(), "Suite", 3, "Luxury", Money.of(new BigDecimal("1200000")));
            when(roomTypeRepo.save(any(RoomType.class))).thenReturn(saved);

            var cmd = new CreateRoomTypeCommand("Suite", 3, "Luxury", new BigDecimal("1200000"));

            // createRoomType() calls HotelContext.current() to stamp the new RoomType.
            // Bind a ScopedValue here as the HotelContextInterceptor would in production.
            RoomTypeResponse[] holder = new RoomTypeResponse[1];
            ScopedValue.where(HotelContext.HOTEL_ID, HotelId.generate())
                    .run(() -> holder[0] = service.createRoomType(cmd));

            assertThat(holder[0].name()).isEqualTo("Suite");
            assertThat(holder[0].capacity()).isEqualTo(3);
            assertThat(holder[0].basePrice()).isEqualByComparingTo("1200000");
            verify(roomTypeRepo).save(any());
        }
    }

    // ── updateRoomType ───────────────────────────────────────────────

    @Nested
    @DisplayName("updateRoomType()")
    class UpdateRoomType {

        @Test
        @DisplayName("throws ResourceNotFoundException when room type not found")
        void throwsWhenNotFound() {
            RoomTypeId id = RoomTypeId.generate();
            when(roomTypeRepo.findById(id)).thenReturn(Optional.empty());

            var cmd = new UpdateRoomTypeCommand("Superior", 2, "Updated", new BigDecimal("600000"));

            assertThatThrownBy(() -> service.updateRoomType(id, cmd))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("updates and returns response when room type exists")
        void updatesExistingRoomType() {
            RoomType rt = stubRoomType();
            when(roomTypeRepo.findById(rt.getId())).thenReturn(Optional.of(rt));
            when(roomTypeRepo.save(any(RoomType.class))).thenReturn(rt);

            var cmd = new UpdateRoomTypeCommand("Deluxe Plus", 3, "Upgraded", new BigDecimal("700000"));
            RoomTypeResponse result = service.updateRoomType(rt.getId(), cmd);

            assertThat(result).isNotNull();
            verify(roomTypeRepo).save(rt);
        }
    }

    // ── findRoomTypeById ─────────────────────────────────────────────

    @Nested
    @DisplayName("findRoomTypeById()")
    class FindRoomTypeById {

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown id")
        void throwsForUnknownId() {
            RoomTypeId id = RoomTypeId.generate();
            when(roomTypeRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findRoomTypeById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("returns response when room type found")
        void returnsResponseWhenFound() {
            RoomType rt = stubRoomType();
            when(roomTypeRepo.findById(rt.getId())).thenReturn(Optional.of(rt));

            RoomTypeResponse result = service.findRoomTypeById(rt.getId());

            assertThat(result.name()).isEqualTo("Deluxe");
        }
    }

    // ── findAllRoomTypes ─────────────────────────────────────────────

    @Nested
    @DisplayName("findAllRoomTypes()")
    class FindAllRoomTypes {

        @Test
        @DisplayName("returns empty page when no room types exist")
        void returnsEmptyPage() {
            when(roomTypeRepo.findAll(page)).thenReturn(List.of());
            when(roomTypeRepo.count()).thenReturn(0L);

            var result = service.findAllRoomTypes(page);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("returns page with room type responses")
        void returnsPageWithRoomTypes() {
            RoomType rt = stubRoomType();
            when(roomTypeRepo.findAll(page)).thenReturn(List.of(rt));
            when(roomTypeRepo.count()).thenReturn(1L);

            var result = service.findAllRoomTypes(page);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().name()).isEqualTo("Deluxe");
        }
    }

    // ── addRateOverride ──────────────────────────────────────────────

    @Nested
    @DisplayName("addRateOverride()")
    class AddRateOverride {

        @Test
        @DisplayName("throws ResourceNotFoundException when room type not found")
        void throwsWhenRoomTypeNotFound() {
            RoomTypeId id = RoomTypeId.generate();
            when(roomTypeRepo.findById(id)).thenReturn(Optional.empty());

            var cmd = new AddRateOverrideCommand(
                    "Weekend Rate", new BigDecimal("650000"),
                    LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31));

            assertThatThrownBy(() -> service.addRateOverride(id, cmd))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(overrideRepo);
        }

        @Test
        @DisplayName("saves override and returns room type response")
        void savesOverrideAndReturnsRoomType() {
            RoomType rt = stubRoomType();
            when(roomTypeRepo.findById(rt.getId())).thenReturn(Optional.of(rt));

            var cmd = new AddRateOverrideCommand(
                    "Weekend Rate", new BigDecimal("650000"),
                    LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31));

            // addRateOverride stamps a RateChangedEvent with HotelContext.current().
            RoomTypeResponse[] holder = new RoomTypeResponse[1];
            ScopedValue.where(HotelContext.HOTEL_ID, HotelId.generate())
                    .run(() -> holder[0] = service.addRateOverride(rt.getId(), cmd));

            assertThat(holder[0].name()).isEqualTo("Deluxe");
            verify(overrideRepo).save(any());
            verify(events).publishEvent(any(id.co.hospitops.shared.event.RateChangedEvent.class));
        }
    }
}
