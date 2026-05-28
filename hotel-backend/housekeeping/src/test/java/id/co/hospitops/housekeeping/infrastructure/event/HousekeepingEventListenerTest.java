package id.co.hospitops.housekeeping.infrastructure.event;

import id.co.hospitops.housekeeping.application.HousekeepingService;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.GuestId;
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
 * Unit tests for HousekeepingEventListener (R-01 fix).
 *
 * <p>Verifies that:
 * <ul>
 *   <li>onCheckout() is annotated with {@code @TransactionalEventListener(AFTER_COMMIT)}
 *       so housekeeping task creation never rolls back the guest's checkout transaction.</li>
 *   <li>onCheckout() delegates the correct roomId and reservationId to
 *       {@link HousekeepingService#createCheckoutTask}.</li>
 *   <li>No other HousekeepingService methods are invoked.</li>
 * </ul>
 */
@DisplayName("HousekeepingEventListener")
@ExtendWith(MockitoExtension.class)
class HousekeepingEventListenerTest {

    @Mock HousekeepingService housekeepingService;
    @InjectMocks HousekeepingEventListener listener;

    // ── annotation contract ───────────────────────────────────────

    @Nested
    @DisplayName("annotation contract")
    class AnnotationContract {

        @Test
        @DisplayName("onCheckout() carries @TransactionalEventListener(AFTER_COMMIT) — R-01 fix")
        void onCheckoutAnnotatedWithAfterCommit() throws NoSuchMethodException {
            Method method = HousekeepingEventListener.class.getMethod(
                    "onCheckout", ReservationCheckedOutEvent.class);
            TransactionalEventListener annotation =
                    method.getAnnotation(TransactionalEventListener.class);

            assertThat(annotation)
                    .as("@TransactionalEventListener must be present on onCheckout()")
                    .isNotNull();
            assertThat(annotation.phase())
                    .as("phase must be AFTER_COMMIT so task creation never rolls back checkout")
                    .isEqualTo(TransactionPhase.AFTER_COMMIT);
        }
    }

    // ── onCheckout ────────────────────────────────────────────────

    @Nested
    @DisplayName("onCheckout()")
    class OnCheckout {

        @Test
        @DisplayName("delegates correct roomId and reservationId to createCheckoutTask()")
        void delegatesWithCorrectArguments() {
            RoomId        roomId        = RoomId.generate();
            ReservationId reservationId = ReservationId.generate();
            ReservationCheckedOutEvent event =
                    new ReservationCheckedOutEvent(reservationId, roomId, GuestId.generate(), 2L);

            listener.onCheckout(event);

            verify(housekeepingService).createCheckoutTask(roomId, reservationId);
        }

        @Test
        @DisplayName("calls createCheckoutTask() exactly once per event")
        void callsServiceExactlyOnce() {
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    ReservationId.generate(), RoomId.generate(), GuestId.generate(), 1L);

            listener.onCheckout(event);

            verify(housekeepingService, times(1)).createCheckoutTask(any(), any());
            verifyNoMoreInteractions(housekeepingService);
        }

        @Test
        @DisplayName("does NOT call any other HousekeepingService method")
        void doesNotCallOtherMethods() {
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    ReservationId.generate(), RoomId.generate(), GuestId.generate(), 3L);

            listener.onCheckout(event);

            verify(housekeepingService).createCheckoutTask(any(), any());
            verifyNoMoreInteractions(housekeepingService);
        }

        @Test
        @DisplayName("roomId from event is passed — not guestId or reservationId")
        void passesRoomIdNotOtherIds() {
            RoomId        expectedRoomId        = RoomId.generate();
            ReservationId expectedReservationId = ReservationId.generate();
            // Different IDs to confirm no accidental transposition
            ReservationCheckedOutEvent event = new ReservationCheckedOutEvent(
                    expectedReservationId, expectedRoomId, GuestId.generate(), 5L);

            listener.onCheckout(event);

            verify(housekeepingService).createCheckoutTask(expectedRoomId, expectedReservationId);
        }
    }
}
