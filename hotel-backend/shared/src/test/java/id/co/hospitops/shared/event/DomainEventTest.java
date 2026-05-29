package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


import static org.assertj.core.api.Assertions.*;

/**
 * R-07 — verifies that DomainEvent subclasses are pure POJOs with no Spring dependency,
 * carry an {@code occurredOn} timestamp, and correctly store their payload.
 */
@DisplayName("DomainEvent")
class DomainEventTest {

    // ── occurredOn ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("occurredOn")
    class OccurredOn {

        @Test
        @DisplayName("is populated at construction time")
        void isPopulatedOnConstruction() {
            Instant before = Instant.now();
            ReservationCreatedEvent event = sampleReservationCreatedEvent();
            Instant after = Instant.now();

            assertThat(event.getOccurredOn())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("two events created sequentially have non-decreasing timestamps")
        void sequentialEventsHaveNonDecreasingTimestamps() {
            ReservationCreatedEvent first = sampleReservationCreatedEvent();
            ReservationCreatedEvent second = sampleReservationCreatedEvent();

            assertThat(second.getOccurredOn())
                    .isAfterOrEqualTo(first.getOccurredOn());
        }
    }

    // ── no Spring dependency ──────────────────────────────────────────────

    @Nested
    @DisplayName("Spring independence")
    class SpringIndependence {

        @Test
        @DisplayName("DomainEvent does NOT extend ApplicationEvent")
        void doesNotExtendApplicationEvent() {
            // If this class is on the classpath the test will still pass — the
            // assertion is that DomainEvent is not a subtype of ApplicationEvent.
            // We check via the class hierarchy rather than loading Spring context.
            Class<?> superclass = DomainEvent.class.getSuperclass();
            assertThat(superclass)
                    .as("DomainEvent must extend Object, not ApplicationEvent")
                    .isEqualTo(Object.class);
        }
    }

    // ── event payloads ────────────────────────────────────────────────────

    @Nested
    @DisplayName("ReservationCreatedEvent")
    class ReservationCreatedEventTests {

        @Test
        @DisplayName("stores all constructor arguments")
        void storesAllFields() {
            ReservationId reservationId = ReservationId.generate();
            RoomId roomId = RoomId.generate();
            GuestId guestId = GuestId.generate();
            LocalDate checkIn = LocalDate.of(2026, 6, 1);
            LocalDate checkOut = LocalDate.of(2026, 6, 5);

            ReservationCreatedEvent event = new ReservationCreatedEvent(
                    reservationId, roomId, guestId, checkIn, checkOut);

            assertThat(event.getReservationId()).isEqualTo(reservationId);
            assertThat(event.getRoomId()).isEqualTo(roomId);
            assertThat(event.getGuestId()).isEqualTo(guestId);
            assertThat(event.getCheckInDate()).isEqualTo(checkIn);
            assertThat(event.getCheckOutDate()).isEqualTo(checkOut);
        }
    }

    @Nested
    @DisplayName("ReservationCheckedInEvent")
    class ReservationCheckedInEventTests {

        @Test
        @DisplayName("stores all constructor arguments")
        void storesAllFields() {
            ReservationId reservationId = ReservationId.generate();
            RoomId roomId = RoomId.generate();
            GuestId guestId = GuestId.generate();

            ReservationCheckedInEvent event =
                    new ReservationCheckedInEvent(reservationId, roomId, guestId);

            assertThat(event.getReservationId()).isEqualTo(reservationId);
            assertThat(event.getRoomId()).isEqualTo(roomId);
            assertThat(event.getGuestId()).isEqualTo(guestId);
        }
    }

    @Nested
    @DisplayName("ReservationCheckedOutEvent")
    class ReservationCheckedOutEventTests {

        @Test
        @DisplayName("stores all constructor arguments including nights")
        void storesAllFields() {
            ReservationId reservationId = ReservationId.generate();
            RoomId roomId = RoomId.generate();
            GuestId guestId = GuestId.generate();
            long nights = 4L;

            ReservationCheckedOutEvent event =
                    new ReservationCheckedOutEvent(reservationId, roomId, guestId, nights);

            assertThat(event.getReservationId()).isEqualTo(reservationId);
            assertThat(event.getRoomId()).isEqualTo(roomId);
            assertThat(event.getGuestId()).isEqualTo(guestId);
            assertThat(event.getNights()).isEqualTo(nights);
        }
    }

    @Nested
    @DisplayName("ReservationCancelledEvent")
    class ReservationCancelledEventTests {

        @Test
        @DisplayName("stores reservationId and roomId")
        void storesAllFields() {
            ReservationId reservationId = ReservationId.generate();
            RoomId roomId = RoomId.generate();

            ReservationCancelledEvent event =
                    new ReservationCancelledEvent(reservationId, roomId);

            assertThat(event.getReservationId()).isEqualTo(reservationId);
            assertThat(event.getRoomId()).isEqualTo(roomId);
        }
    }

    @Nested
    @DisplayName("HousekeepingTaskCreatedEvent")
    class HousekeepingTaskCreatedEventTests {

        @Test
        @DisplayName("stores taskId and roomId")
        void storesAllFields() {
            UUID taskId = UUID.randomUUID();
            RoomId roomId = RoomId.generate();

            HousekeepingTaskCreatedEvent event =
                    new HousekeepingTaskCreatedEvent(taskId, roomId);

            assertThat(event.getTaskId()).isEqualTo(taskId);
            assertThat(event.getRoomId()).isEqualTo(roomId);
        }
    }

    @Nested
    @DisplayName("PaymentReceivedEvent")
    class PaymentReceivedEventTests {

        @Test
        @DisplayName("stores all constructor arguments")
        void storesAllFields() {
            InvoiceId invoiceId = InvoiceId.generate();
            ReservationId reservationId = ReservationId.generate();
            Money amount = Money.of(500_000L);

            PaymentReceivedEvent event =
                    new PaymentReceivedEvent(invoiceId, reservationId, amount, true);

            assertThat(event.getInvoiceId()).isEqualTo(invoiceId);
            assertThat(event.getReservationId()).isEqualTo(reservationId);
            assertThat(event.getAmount()).isEqualTo(amount);
            assertThat(event.isFullyPaid()).isTrue();
        }

        @Test
        @DisplayName("fullyPaid=false when payment is partial")
        void partialPaymentStoresFalse() {
            PaymentReceivedEvent event = new PaymentReceivedEvent(
                    InvoiceId.generate(), ReservationId.generate(),
                    Money.of(100_000L), false);

            assertThat(event.isFullyPaid()).isFalse();
        }
    }

    @Nested
    @DisplayName("HotelCreatedEvent")
    class HotelCreatedEventTests {

        @Test
        @DisplayName("stores hotelId and groupId")
        void storesAllFields() {
            HotelId hotelId = HotelId.generate();
            GroupId groupId = GroupId.generate();

            HotelCreatedEvent event = new HotelCreatedEvent(hotelId, groupId);

            assertThat(event.getHotelId()).isEqualTo(hotelId);
            assertThat(event.getGroupId()).isEqualTo(groupId);
        }

        @Test
        @DisplayName("occurredOn is populated")
        void occurredOnIsPopulated() {
            Instant before = Instant.now();
            HotelCreatedEvent event = new HotelCreatedEvent(HotelId.generate(), GroupId.generate());
            Instant after = Instant.now();

            assertThat(event.getOccurredOn())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
    }

    @Nested
    @DisplayName("HotelActivatedEvent")
    class HotelActivatedEventTests {

        @Test
        @DisplayName("stores hotelId")
        void storesAllFields() {
            HotelId hotelId = HotelId.generate();

            HotelActivatedEvent event = new HotelActivatedEvent(hotelId);

            assertThat(event.getHotelId()).isEqualTo(hotelId);
        }

        @Test
        @DisplayName("occurredOn is populated")
        void occurredOnIsPopulated() {
            Instant before = Instant.now();
            HotelActivatedEvent event = new HotelActivatedEvent(HotelId.generate());
            Instant after = Instant.now();

            assertThat(event.getOccurredOn())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
    }

    @Nested
    @DisplayName("HotelSuspendedEvent")
    class HotelSuspendedEventTests {

        @Test
        @DisplayName("stores hotelId")
        void storesAllFields() {
            HotelId hotelId = HotelId.generate();

            HotelSuspendedEvent event = new HotelSuspendedEvent(hotelId);

            assertThat(event.getHotelId()).isEqualTo(hotelId);
        }

        @Test
        @DisplayName("occurredOn is populated")
        void occurredOnIsPopulated() {
            Instant before = Instant.now();
            HotelSuspendedEvent event = new HotelSuspendedEvent(HotelId.generate());
            Instant after = Instant.now();

            assertThat(event.getOccurredOn())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private ReservationCreatedEvent sampleReservationCreatedEvent() {
        return new ReservationCreatedEvent(
                ReservationId.generate(), RoomId.generate(), GuestId.generate(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3));
    }
}
