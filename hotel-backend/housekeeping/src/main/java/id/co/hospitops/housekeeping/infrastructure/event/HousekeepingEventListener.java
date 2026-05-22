package id.co.hospitops.housekeeping.infrastructure.event;

import id.co.hospitops.housekeeping.application.HousekeepingService;
import id.co.hospitops.shared.event.ReservationCheckedOutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HousekeepingEventListener {

    private final HousekeepingService housekeepingService;

    @EventListener
    @Transactional
    public void onCheckout(ReservationCheckedOutEvent event) {
        log.info("Creating housekeeping task for room {} after checkout of reservation {}",
                event.getRoomId(), event.getReservationId());
        housekeepingService.createCheckoutTask(
                event.getRoomId(), event.getReservationId());
    }
}
