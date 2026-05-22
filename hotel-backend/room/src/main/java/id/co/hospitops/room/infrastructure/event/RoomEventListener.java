package id.co.hospitops.room.infrastructure.event;

import id.co.hospitops.room.application.RoomService;
import id.co.hospitops.shared.event.ReservationCheckedInEvent;
import id.co.hospitops.shared.event.ReservationCheckedOutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to reservation domain events and updates room status automatically.
 *
 * <p>Cancellation events are intentionally not handled here: a reservation can
 * only be cancelled while in PENDING or CONFIRMED state, meaning the room is
 * still AVAILABLE — no status transition is required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEventListener {

    private final RoomService roomService;

    @EventListener
    @Transactional
    public void onCheckedIn(ReservationCheckedInEvent e) {
        log.info("Room {} -> OCCUPIED (reservation {})", e.getRoomId(), e.getReservationId());
        roomService.markOccupied(e.getRoomId());
    }

    @EventListener
    @Transactional
    public void onCheckedOut(ReservationCheckedOutEvent e) {
        log.info("Room {} -> DIRTY (reservation {})", e.getRoomId(), e.getReservationId());
        roomService.markDirty(e.getRoomId());
    }
}
