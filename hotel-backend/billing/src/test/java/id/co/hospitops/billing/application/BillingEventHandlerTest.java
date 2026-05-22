package id.co.hospitops.billing.application;

import id.co.hospitops.billing.domain.port.in.BillingUseCase;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.event.ReservationCheckedOutEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for BillingEventHandler (R3-03 fix).
 *
 * BillingEventHandler was extracted from BillingService to separate the
 * @EventListener concern from business logic (SRP). These tests verify that:
 *   - onCheckout() delegates to BillingUseCase.createInvoiceForCheckout()
 *   - The correct reservationId and nights are passed through
 *   - No business logic is performed inside the handler itself
 */
@DisplayName("BillingEventHandler")
@ExtendWith(MockitoExtension.class)
class BillingEventHandlerTest {

    @Mock  BillingUseCase billingUseCase;
    @InjectMocks BillingEventHandler handler;

    // ── onCheckout ────────────────────────────────────────────────

    @Nested
    @DisplayName("onCheckout()")
    class OnCheckout {

        @Test
        @DisplayName("delegates to createInvoiceForCheckout() with correct arguments")
        void delegatesWithCorrectArguments() {
            ReservationId reservationId = ReservationId.generate();
            long nights = 3L;
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    this, reservationId, RoomId.generate(), GuestId.generate(), nights);

            handler.onCheckout(event);

            verify(billingUseCase).createInvoiceForCheckout(reservationId, nights);
        }

        @Test
        @DisplayName("passes nights = 1 for a single-night stay")
        void passesOneNightStay() {
            ReservationId reservationId = ReservationId.generate();
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    this, reservationId, RoomId.generate(), GuestId.generate(), 1L);

            handler.onCheckout(event);

            verify(billingUseCase).createInvoiceForCheckout(reservationId, 1L);
        }

        @Test
        @DisplayName("passes nights = 30 for a long stay")
        void passesLongStay() {
            ReservationId reservationId = ReservationId.generate();
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    this, reservationId, RoomId.generate(), GuestId.generate(), 30L);

            handler.onCheckout(event);

            verify(billingUseCase).createInvoiceForCheckout(reservationId, 30L);
        }

        @Test
        @DisplayName("calls createInvoiceForCheckout() exactly once per event")
        void callsServiceExactlyOnce() {
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    this, ReservationId.generate(), RoomId.generate(), GuestId.generate(), 2L);

            handler.onCheckout(event);

            verify(billingUseCase, times(1))
                    .createInvoiceForCheckout(any(), anyLong());
            verifyNoMoreInteractions(billingUseCase);
        }

        @Test
        @DisplayName("does NOT call any other BillingUseCase method")
        void doesNotCallOtherMethods() {
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    this, ReservationId.generate(), RoomId.generate(), GuestId.generate(), 2L);

            handler.onCheckout(event);

            verifyNoMoreInteractions(billingUseCase);
        }
    }
}
