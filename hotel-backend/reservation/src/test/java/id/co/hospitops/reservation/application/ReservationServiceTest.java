package id.co.hospitops.reservation.application;

import id.co.hospitops.reservation.application.command.CreateReservationCommand;
import id.co.hospitops.reservation.application.response.ReservationResponse;
import id.co.hospitops.reservation.domain.model.Reservation;
import id.co.hospitops.reservation.domain.model.ReservationStatus;
import id.co.hospitops.reservation.domain.port.out.*;
import id.co.hospitops.reservation.domain.port.out.DisplayEnrichmentPort.GuestDisplay;
import id.co.hospitops.reservation.domain.port.out.DisplayEnrichmentPort.RoomDisplay;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.event.*;
import id.co.hospitops.shared.exception.*;
import id.co.hospitops.shared.web.PageResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReservationService.
 *
 * Covers:
 *   - create()            : guest-not-found, room-not-available, happy path + event published
 *   - checkIn()           : not-found, wrong-status guard, happy path + event published
 *   - checkOut()          : not-found, wrong-status guard, happy path + event (nights) published
 *   - cancel()            : not-found, already-checked-in guard, PENDING path, happy path + event
 *   - findAll()           : null/blank filter, valid statuses, invalid → BusinessRuleViolation (W-12 / C-2 fix)
 *   - findByGuest()       : real countByGuestId delegation (W-6 fix)
 *   - todayArrivals/Departures: repository delegation
 */
@DisplayName("ReservationService")
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository      reservationRepo;
    @Mock RoomAvailabilityPort       roomAvailability;
    @Mock GuestValidationPort        guestValidation;
    @Mock ReservationNumberGenerator numberGenerator;
    @Mock ApplicationEventPublisher  eventPublisher;
    @Mock DisplayEnrichmentPort      displayEnrichment;

    @InjectMocks ReservationService service;

    /** Minimal display stubs used by all tests that reach enrich() and return a ReservationResponse. */
    private void stubEnrichment() {
        when(displayEnrichment.findGuestDisplay(any())).thenReturn(new GuestDisplay("Test Guest", "1234567890"));
        when(displayEnrichment.findRoomDisplay(any())).thenReturn(new RoomDisplay("101", "Deluxe"));
    }

    // ── helpers ────────────────────────────────────────────────────

    private static final LocalDate CHECK_IN  = LocalDate.now().plusDays(1);
    private static final LocalDate CHECK_OUT = LocalDate.now().plusDays(3); // 2 nights
    private static final Money     RATE      = Money.of(new BigDecimal("200000"));

    private static Reservation stubConfirmed() {
        return Reservation.create("RES-001", GuestId.generate(), RoomId.generate(),
                CHECK_IN, CHECK_OUT, RATE, 2, 0, null, StaffId.generate());
    }

    private static Reservation stubCheckedIn() {
        Reservation r = stubConfirmed();
        r.checkIn();
        return r;
    }

    private static CreateReservationCommand stubCommand(GuestId guestId, RoomId roomId) {
        return new CreateReservationCommand(guestId, roomId, CHECK_IN, CHECK_OUT,
                2, 0, "Sea view please", StaffId.generate());
    }

    // ══════════════════════════════════════════════════════════════
    // create()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("throws ResourceNotFoundException when guest does not exist")
        void throwsWhenGuestNotFound() {
            GuestId guestId = GuestId.generate();
            RoomId  roomId  = RoomId.generate();
            when(guestValidation.exists(guestId)).thenReturn(false);

            assertThatThrownBy(() -> service.create(stubCommand(guestId, roomId)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(roomAvailability, reservationRepo, eventPublisher);
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when room is not available")
        void throwsWhenRoomNotAvailable() {
            GuestId guestId = GuestId.generate();
            RoomId  roomId  = RoomId.generate();
            when(guestValidation.exists(guestId)).thenReturn(true);
            when(roomAvailability.isAvailable(roomId, CHECK_IN, CHECK_OUT)).thenReturn(false);

            assertThatThrownBy(() -> service.create(stubCommand(guestId, roomId)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("not available");

            verifyNoInteractions(reservationRepo, eventPublisher);
        }

        @Test
        @DisplayName("saves reservation and publishes ReservationCreatedEvent on success")
        void savesAndPublishesCreatedEvent() {
            GuestId    guestId = GuestId.generate();
            RoomId     roomId  = RoomId.generate();
            Reservation saved   = stubConfirmed();

            when(guestValidation.exists(guestId)).thenReturn(true);
            when(roomAvailability.isAvailable(roomId, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(roomAvailability.resolveRate(eq(roomId), any())).thenReturn(RATE);
            when(numberGenerator.generate()).thenReturn("RES-001");
            when(reservationRepo.save(any(Reservation.class))).thenReturn(saved);
            stubEnrichment();

            ReservationResponse result = service.create(stubCommand(guestId, roomId));

            assertThat(result).isNotNull();
            verify(reservationRepo).save(any(Reservation.class));
            verify(eventPublisher).publishEvent(any(ReservationCreatedEvent.class));
        }

        @Test
        @DisplayName("does not call repository when room availability check fails")
        void noRepoCallWhenRoomUnavailable() {
            GuestId guestId = GuestId.generate();
            RoomId  roomId  = RoomId.generate();
            when(guestValidation.exists(guestId)).thenReturn(true);
            when(roomAvailability.isAvailable(roomId, CHECK_IN, CHECK_OUT)).thenReturn(false);

            assertThatThrownBy(() -> service.create(stubCommand(guestId, roomId)))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verifyNoInteractions(reservationRepo);
        }

        @Test
        @DisplayName("translates DataIntegrityViolationException to ConflictException (R-03: DB exclusion constraint race)")
        void translatesConstraintViolationToConflict() {
            // Simulates the TOCTOU scenario: availability check passes for two concurrent
            // requests, but the DB exclusion constraint (V8 migration) rejects the second
            // insert. The service must surface a ConflictException (HTTP 409) rather than
            // letting a raw DataIntegrityViolationException propagate as HTTP 500.
            GuestId guestId = GuestId.generate();
            RoomId  roomId  = RoomId.generate();
            when(guestValidation.exists(guestId)).thenReturn(true);
            when(roomAvailability.isAvailable(roomId, CHECK_IN, CHECK_OUT)).thenReturn(true);
            when(roomAvailability.resolveRate(eq(roomId), any())).thenReturn(RATE);
            when(numberGenerator.generate()).thenReturn("RES-999");
            when(reservationRepo.save(any(Reservation.class)))
                    .thenThrow(new DataIntegrityViolationException("uq_room_no_overlap"));

            assertThatThrownBy(() -> service.create(stubCommand(guestId, roomId)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("no longer available");

            verifyNoInteractions(eventPublisher);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // checkIn()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkIn()")
    class CheckIn {

        @Test
        @DisplayName("throws ResourceNotFoundException when reservation does not exist")
        void throwsWhenNotFound() {
            ReservationId id = ReservationId.generate();
            when(reservationRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checkIn(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when reservation is not CONFIRMED")
        void throwsForWrongStatus() {
            ReservationId id        = ReservationId.generate();
            Reservation   checkedIn = stubCheckedIn(); // already CHECKED_IN — cannot check in again
            when(reservationRepo.findById(id)).thenReturn(Optional.of(checkedIn));

            assertThatThrownBy(() -> service.checkIn(id))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("saves updated reservation and publishes ReservationCheckedInEvent")
        void savesAndPublishesCheckedInEvent() {
            ReservationId id        = ReservationId.generate();
            Reservation   confirmed = stubConfirmed();
            when(reservationRepo.findById(id)).thenReturn(Optional.of(confirmed));
            when(reservationRepo.save(confirmed)).thenReturn(confirmed);
            stubEnrichment();

            ReservationResponse result = service.checkIn(id);

            assertThat(result).isNotNull();
            verify(reservationRepo).save(confirmed);
            verify(eventPublisher).publishEvent(any(ReservationCheckedInEvent.class));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // checkOut()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkOut()")
    class CheckOut {

        @Test
        @DisplayName("throws ResourceNotFoundException when reservation does not exist")
        void throwsWhenNotFound() {
            ReservationId id = ReservationId.generate();
            when(reservationRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checkOut(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when reservation is CONFIRMED (not CHECKED_IN)")
        void throwsForWrongStatus() {
            ReservationId id        = ReservationId.generate();
            Reservation   confirmed = stubConfirmed(); // CONFIRMED, not CHECKED_IN
            when(reservationRepo.findById(id)).thenReturn(Optional.of(confirmed));

            assertThatThrownBy(() -> service.checkOut(id))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("publishes ReservationCheckedOutEvent with correct nights value")
        void publishesEventWithCorrectNights() {
            ReservationId id    = ReservationId.generate();
            Reservation   ready = stubCheckedIn(); // CHECK_OUT - CHECK_IN = 2 nights
            when(reservationRepo.findById(id)).thenReturn(Optional.of(ready));
            when(reservationRepo.save(ready)).thenReturn(ready);
            stubEnrichment();

            service.checkOut(id);

            ArgumentCaptor<ReservationCheckedOutEvent> captor =
                    ArgumentCaptor.forClass(ReservationCheckedOutEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getNights()).isEqualTo(2L);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // cancel()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("throws ResourceNotFoundException when reservation does not exist")
        void throwsWhenNotFound() {
            ReservationId id = ReservationId.generate();
            when(reservationRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancel(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when reservation is CHECKED_IN (canCancel = false)")
        void throwsForCheckedInStatus() {
            ReservationId id        = ReservationId.generate();
            Reservation   checkedIn = stubCheckedIn();
            when(reservationRepo.findById(id)).thenReturn(Optional.of(checkedIn));

            assertThatThrownBy(() -> service.cancel(id))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("saves and publishes ReservationCancelledEvent for CONFIRMED reservation")
        void cancelsConfirmedReservation() {
            ReservationId id        = ReservationId.generate();
            Reservation   confirmed = stubConfirmed();
            when(reservationRepo.findById(id)).thenReturn(Optional.of(confirmed));
            when(reservationRepo.save(confirmed)).thenReturn(confirmed);
            stubEnrichment();

            service.cancel(id);

            verify(reservationRepo).save(confirmed);
            verify(eventPublisher).publishEvent(any(ReservationCancelledEvent.class));
        }

        @Test
        @DisplayName("can cancel a PENDING reservation")
        void cancelsPendingReservation() {
            ReservationId id      = ReservationId.generate();
            Reservation   pending = Reservation.reconstitute(
                    ReservationId.generate(), "RES-002",
                    GuestId.generate(), RoomId.generate(),
                    CHECK_IN, CHECK_OUT, ReservationStatus.PENDING,
                    RATE, 1, 0, null, StaffId.generate(),
                    LocalDateTime.now(), LocalDateTime.now());
            when(reservationRepo.findById(id)).thenReturn(Optional.of(pending));
            when(reservationRepo.save(pending)).thenReturn(pending);
            stubEnrichment();

            assertThatNoException().isThrownBy(() -> service.cancel(id));
            verify(eventPublisher).publishEvent(any(ReservationCancelledEvent.class));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // findAll() — W-12 + C-2 fixes
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll() — status filter")
    class FindAll {

        private final Pageable page = PageRequest.of(0, 10);

        @Test
        @DisplayName("null statusFilter delegates to findAll() (C-2 fix: not findByGuestId(null))")
        void nullFilterFetchesAll() {
            when(reservationRepo.findAll(page)).thenReturn(List.of());
            when(reservationRepo.count()).thenReturn(0L);

            PageResult<ReservationResponse> result = service.findAll(null, page);

            assertThat(result.content()).isEmpty();
            verify(reservationRepo).findAll(page);
            verify(reservationRepo, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("blank statusFilter also delegates to findAll()")
        void blankFilterFetchesAll() {
            when(reservationRepo.findAll(page)).thenReturn(List.of());
            when(reservationRepo.count()).thenReturn(0L);

            service.findAll("   ", page);

            verify(reservationRepo).findAll(page);
            verify(reservationRepo, never()).findByStatus(any(), any());
        }

        @ParameterizedTest(name = "status = {0}")
        @ValueSource(strings = {"PENDING", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"})
        @DisplayName("valid status filter delegates to findByStatus()")
        void validStatusDelegatesToFindByStatus(String validStatus) {
            when(reservationRepo.findByStatus(any(), eq(page))).thenReturn(List.of());
            when(reservationRepo.count()).thenReturn(0L);

            service.findAll(validStatus, page);

            verify(reservationRepo).findByStatus(ReservationStatus.valueOf(validStatus), page);
            verify(reservationRepo, never()).findAll(page);
        }

        @Test
        @DisplayName("status filter is case-insensitive (lowercase input)")
        void caseInsensitiveFilter() {
            when(reservationRepo.findByStatus(eq(ReservationStatus.CONFIRMED), eq(page))).thenReturn(List.of());
            when(reservationRepo.count()).thenReturn(0L);

            assertThatNoException().isThrownBy(() -> service.findAll("confirmed", page));
            verify(reservationRepo).findByStatus(ReservationStatus.CONFIRMED, page);
        }

        @ParameterizedTest(name = "statusFilter = \"{0}\"")
        @ValueSource(strings = {"UNKNOWN", "ACTIVE", "RESERVED", "pending_payment", "123"})
        @DisplayName("unknown status throws BusinessRuleViolationException — never reaches repo (W-12 fix)")
        void unknownStatusThrowsBusinessRuleViolation(String badStatus) {
            assertThatThrownBy(() -> service.findAll(badStatus, page))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(badStatus);

            verifyNoInteractions(reservationRepo);
        }

        @Test
        @DisplayName("exception message includes the offending value")
        void exceptionMessageIncludesOffendingValue() {
            assertThatThrownBy(() -> service.findAll("GARBAGE", page))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("GARBAGE");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // findByGuest() — W-6 fix: real countByGuestId
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByGuest() — pagination total (W-6 fix)")
    class FindByGuest {

        private final Pageable page = PageRequest.of(0, 5);

        @Test
        @DisplayName("uses countByGuestId() for total — not the global count() (W-6 fix)")
        void usesRealCountByGuestId() {
            GuestId guestId = GuestId.generate();
            when(reservationRepo.findByGuestId(guestId, page)).thenReturn(List.of());
            when(reservationRepo.countByGuestId(guestId)).thenReturn(7L);

            PageResult<ReservationResponse> result = service.findByGuest(guestId, page);

            assertThat(result.totalElements()).isEqualTo(7L);
            verify(reservationRepo).findByGuestId(guestId, page);
            verify(reservationRepo).countByGuestId(guestId);
            verify(reservationRepo, never()).count(); // must NOT call the global count
        }

        @Test
        @DisplayName("returns empty page when guest has no reservations")
        void emptyWhenNoReservations() {
            GuestId guestId = GuestId.generate();
            when(reservationRepo.findByGuestId(guestId, page)).thenReturn(List.of());
            when(reservationRepo.countByGuestId(guestId)).thenReturn(0L);

            PageResult<ReservationResponse> result = service.findByGuest(guestId, page);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // todayArrivals() / todayDepartures()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("todayArrivals() and todayDepartures()")
    class TodayLists {

        @Test
        @DisplayName("todayArrivals() delegates to findTodayArrivals() with today's date")
        void arrivalsUsesTodayDate() {
            when(reservationRepo.findTodayArrivals(any(LocalDate.class))).thenReturn(List.of());

            List<ReservationResponse> result = service.todayArrivals();

            assertThat(result).isEmpty();
            verify(reservationRepo).findTodayArrivals(any(LocalDate.class));
        }

        @Test
        @DisplayName("todayDepartures() delegates to findTodayDepartures() with today's date")
        void departuresUsesTodayDate() {
            when(reservationRepo.findTodayDepartures(any(LocalDate.class))).thenReturn(List.of());

            List<ReservationResponse> result = service.todayDepartures();

            assertThat(result).isEmpty();
            verify(reservationRepo).findTodayDepartures(any(LocalDate.class));
        }
    }
}
