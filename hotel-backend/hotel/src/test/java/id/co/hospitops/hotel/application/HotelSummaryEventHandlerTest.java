package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.model.HotelSummary;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.hotel.domain.port.out.HotelSummaryRepository;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.InvoiceId;
import id.co.hospitops.shared.event.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HotelSummaryEventHandler}.
 *
 * <p>Each test verifies that the correct mutator is called on the summary model
 * in response to a specific domain event, and that the updated summary is persisted.
 */
@DisplayName("HotelSummaryEventHandler")
@ExtendWith(MockitoExtension.class)
class HotelSummaryEventHandlerTest {

    @Mock
    HotelRepository hotelRepo;
    @Mock
    HotelSummaryRepository summaryRepo;
    @InjectMocks
    HotelSummaryEventHandler handler;

    private final HotelId hotelId = HotelId.generate();
    private final RoomId roomId = RoomId.generate();

    private HotelSummary summaryWithValues(int occupied, int total, int dirty,
                                           int arrivals, int departures,
                                           BigDecimal revenueToday) {
        return HotelSummary.reconstitute(hotelId, "Test Hotel", occupied, total,
                arrivals, departures, revenueToday, BigDecimal.ZERO, dirty, java.time.LocalDateTime.now());
    }

    @BeforeEach
    void stubSave() {
        // Always return whatever is passed to save() so callers can inspect the saved value.
        when(summaryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Stubs a default empty summary for {@link #hotelId}. Call in tests that invoke getOrCreate().
     */
    private void stubExistingSummary() {
        when(summaryRepo.findByHotelId(hotelId)).thenReturn(Optional.of(HotelSummary.empty(hotelId, "Test Hotel")));
    }

    // ── RoomCreatedEvent ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("onRoomCreated()")
    class OnRoomCreated {

        @Test
        @DisplayName("increments totalRooms by 1")
        void incrementsTotalRooms() {
            HotelSummary existing = summaryWithValues(0, 5, 0, 0, 0, BigDecimal.ZERO);
            when(summaryRepo.findByHotelId(hotelId)).thenReturn(Optional.of(existing));

            handler.onRoomCreated(new RoomCreatedEvent(hotelId, roomId));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getTotalRooms()).isEqualTo(6);
        }

        @Test
        @DisplayName("creates empty summary when no row exists (new hotel edge case)")
        void createsEmptySummaryWhenAbsent() {
            when(summaryRepo.findByHotelId(hotelId)).thenReturn(Optional.empty());

            handler.onRoomCreated(new RoomCreatedEvent(hotelId, roomId));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getTotalRooms()).isEqualTo(1);
        }
    }

    // ── ReservationCreatedEvent ───────────────────────────────────────────────

    @Nested
    @DisplayName("onReservationCreated()")
    class OnReservationCreated {

        @Test
        @DisplayName("increments arrivalsToday when check-in is today")
        void incrementsArrivalsWhenCheckInIsToday() {
            stubExistingSummary();
            handler.onReservationCreated(new ReservationCreatedEvent(
                    hotelId, ReservationId.generate(), roomId, id.co.hospitops.shared.GuestId.generate(),
                    LocalDate.now(), LocalDate.now().plusDays(3)));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getArrivalsToday()).isEqualTo(1);
            assertThat(cap.getValue().getDeparturesToday()).isZero();
        }

        @Test
        @DisplayName("increments departuresToday when check-out is today")
        void incrementsDeparturesWhenCheckOutIsToday() {
            stubExistingSummary();
            handler.onReservationCreated(new ReservationCreatedEvent(
                    hotelId, ReservationId.generate(), roomId, id.co.hospitops.shared.GuestId.generate(),
                    LocalDate.now().minusDays(2), LocalDate.now()));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getDeparturesToday()).isEqualTo(1);
        }

        @Test
        @DisplayName("does not increment arrivals when check-in is in the future")
        void noArrivalsWhenCheckInIsFuture() {
            stubExistingSummary();
            handler.onReservationCreated(new ReservationCreatedEvent(
                    hotelId, ReservationId.generate(), roomId, id.co.hospitops.shared.GuestId.generate(),
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(4)));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getArrivalsToday()).isZero();
            assertThat(cap.getValue().getDeparturesToday()).isZero();
        }
    }

    // ── ReservationCheckedInEvent ─────────────────────────────────────────────

    @Nested
    @DisplayName("onCheckedIn()")
    class OnCheckedIn {

        @Test
        @DisplayName("increments occupiedRooms by 1")
        void incrementsOccupied() {
            HotelSummary existing = summaryWithValues(2, 10, 0, 0, 0, BigDecimal.ZERO);
            when(summaryRepo.findByHotelId(hotelId)).thenReturn(Optional.of(existing));

            handler.onCheckedIn(new ReservationCheckedInEvent(
                    hotelId, ReservationId.generate(), roomId, id.co.hospitops.shared.GuestId.generate()));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getOccupiedRooms()).isEqualTo(3);
        }
    }

    // ── ReservationCheckedOutEvent ────────────────────────────────────────────

    @Nested
    @DisplayName("onCheckedOut()")
    class OnCheckedOut {

        @Test
        @DisplayName("decrements occupiedRooms by 1")
        void decrementsOccupied() {
            HotelSummary existing = summaryWithValues(3, 10, 0, 0, 0, BigDecimal.ZERO);
            when(summaryRepo.findByHotelId(hotelId)).thenReturn(Optional.of(existing));

            handler.onCheckedOut(new ReservationCheckedOutEvent(
                    hotelId, ReservationId.generate(), roomId,
                    id.co.hospitops.shared.GuestId.generate(), 2L));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getOccupiedRooms()).isEqualTo(2);
        }

        @Test
        @DisplayName("occupiedRooms never goes below zero")
        void neverBelowZero() {
            stubExistingSummary();
            // occupiedRooms is already 0
            handler.onCheckedOut(new ReservationCheckedOutEvent(
                    hotelId, ReservationId.generate(), roomId,
                    id.co.hospitops.shared.GuestId.generate(), 1L));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getOccupiedRooms()).isZero();
        }
    }

    // ── PaymentReceivedEvent ──────────────────────────────────────────────────

    @Nested
    @DisplayName("onPaymentReceived()")
    class OnPaymentReceived {

        @Test
        @DisplayName("adds payment amount to revenueToday and revenueMonth")
        void addsRevenueToTodayAndMonth() {
            stubExistingSummary();
            handler.onPaymentReceived(new PaymentReceivedEvent(
                    hotelId, InvoiceId.generate(), ReservationId.generate(),
                    Money.of(new BigDecimal("150000")), false));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getRevenueToday()).isEqualByComparingTo("150000");
            assertThat(cap.getValue().getRevenueMonth()).isEqualByComparingTo("150000");
        }

        @Test
        @DisplayName("accumulates multiple payments")
        void accumulatesMultiplePayments() {
            HotelSummary existing = summaryWithValues(0, 0, 0, 0, 0, new BigDecimal("100000"));
            when(summaryRepo.findByHotelId(hotelId)).thenReturn(Optional.of(existing));

            handler.onPaymentReceived(new PaymentReceivedEvent(
                    hotelId, InvoiceId.generate(), ReservationId.generate(),
                    Money.of(new BigDecimal("50000")), true));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getRevenueToday()).isEqualByComparingTo("150000");
        }
    }

    // ── HousekeepingTaskCreatedEvent ──────────────────────────────────────────

    @Nested
    @DisplayName("onHousekeepingTaskCreated()")
    class OnHousekeepingTaskCreated {

        @Test
        @DisplayName("increments dirtyRooms by 1")
        void incrementsDirtyRooms() {
            HotelSummary existing = summaryWithValues(0, 10, 2, 0, 0, BigDecimal.ZERO);
            when(summaryRepo.findByHotelId(hotelId)).thenReturn(Optional.of(existing));

            handler.onHousekeepingTaskCreated(
                    new HousekeepingTaskCreatedEvent(hotelId, UUID.randomUUID(), roomId));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getDirtyRooms()).isEqualTo(3);
        }
    }

    // ── HotelCreatedEvent ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("onHotelCreated()")
    class OnHotelCreated {

        @Test
        @DisplayName("seeds an empty summary row for the new hotel, including the hotel name")
        void seedsEmptySummary() {
            HotelId newHotel = HotelId.generate();
            Hotel hotelStub = mock(Hotel.class);
            when(hotelStub.getName()).thenReturn("Grand Test Hotel");
            when(hotelRepo.findById(newHotel)).thenReturn(Optional.of(hotelStub));

            handler.onHotelCreated(new HotelCreatedEvent(newHotel, GroupId.generate()));

            ArgumentCaptor<HotelSummary> cap = ArgumentCaptor.forClass(HotelSummary.class);
            verify(summaryRepo).save(cap.capture());
            assertThat(cap.getValue().getHotelId()).isEqualTo(newHotel);
            assertThat(cap.getValue().getHotelName()).isEqualTo("Grand Test Hotel");
            assertThat(cap.getValue().getTotalRooms()).isZero();
            assertThat(cap.getValue().getOccupiedRooms()).isZero();
        }
    }
}
