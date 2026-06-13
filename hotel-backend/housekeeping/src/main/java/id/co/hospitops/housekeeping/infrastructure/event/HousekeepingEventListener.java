package id.co.hospitops.housekeeping.infrastructure.event;

import id.co.hospitops.housekeeping.application.HousekeepingService;
import id.co.hospitops.shared.event.ReservationCheckedOutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class HousekeepingEventListener {

    private final HousekeepingService housekeepingService;

    /**
     * R-01 FIX: Runs AFTER the checkout transaction commits so a housekeeping task
     * creation failure never rolls back the guest's checkout.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckout(ReservationCheckedOutEvent event) {
        log.info("Creating housekeeping task for room {} after checkout of reservation {}",
                event.getRoomId(), event.getReservationId());
        housekeepingService.createCheckoutTask(
                event.getHotelId(), event.getRoomId(), event.getReservationId());
    }
}
