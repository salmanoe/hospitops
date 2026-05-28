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
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;
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

    // ── annotation metadata ───────────────────────────────────────

    @Nested
    @DisplayName("annotation contract")
    class AnnotationContract {

        @Test
        @DisplayName("onCheckout() carries @TransactionalEventListener(AFTER_COMMIT) — R-01 fix")
        void onCheckoutAnnotatedWithAfterCommit() throws NoSuchMethodException {
            Method method = BillingEventHandler.class.getMethod(
                    "onCheckout", ReservationCheckedOutEvent.class);
            TransactionalEventListener annotation =
                    method.getAnnotation(TransactionalEventListener.class);

            assertThat(annotation)
                    .as("@TransactionalEventListener must be present on onCheckout()")
                    .isNotNull();
            assertThat(annotation.phase())
                    .as("phase must be AFTER_COMMIT so invoice creation never rolls back checkout")
                    .isEqualTo(TransactionPhase.AFTER_COMMIT);
        }
    }

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
                    reservationId, RoomId.generate(), GuestId.generate(), nights);

            handler.onCheckout(event);

            verify(billingUseCase).createInvoiceForCheckout(reservationId, nights);
        }

        @Test
        @DisplayName("passes nights = 1 for a single-night stay")
        void passesOneNightStay() {
            ReservationId reservationId = ReservationId.generate();
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    reservationId, RoomId.generate(), GuestId.generate(), 1L);

            handler.onCheckout(event);

            verify(billingUseCase).createInvoiceForCheckout(reservationId, 1L);
        }

        @Test
        @DisplayName("passes nights = 30 for a long stay")
        void passesLongStay() {
            ReservationId reservationId = ReservationId.generate();
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    reservationId, RoomId.generate(), GuestId.generate(), 30L);

            handler.onCheckout(event);

            verify(billingUseCase).createInvoiceForCheckout(reservationId, 30L);
        }

        @Test
        @DisplayName("calls createInvoiceForCheckout() exactly once per event")
        void callsServiceExactlyOnce() {
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    ReservationId.generate(), RoomId.generate(), GuestId.generate(), 2L);

            handler.onCheckout(event);

            verify(billingUseCase, times(1))
                    .createInvoiceForCheckout(any(), anyLong());
            verifyNoMoreInteractions(billingUseCase);
        }

        @Test
        @DisplayName("does NOT call any other BillingUseCase method")
        void doesNotCallOtherMethods() {
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    ReservationId.generate(), RoomId.generate(), GuestId.generate(), 2L);

            handler.onCheckout(event);

            // Verify the one expected call, then assert nothing else was invoked.
            verify(billingUseCase).createInvoiceForCheckout(any(), anyLong());
            verifyNoMoreInteractions(billingUseCase);
        }
    }
}
