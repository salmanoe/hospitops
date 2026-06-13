package id.co.hospitops.channel.infrastructure.event;

import id.co.hospitops.channel.application.ChannelSyncService;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.event.ReservationCancelledEvent;
import id.co.hospitops.shared.event.ReservationCheckedOutEvent;
import id.co.hospitops.shared.event.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Pushes availability to the channel manager whenever a reservation changes the
 * inventory of a room type. Runs synchronously inside the reservation
 * transaction, so the outbox write is atomic with the booking change.
 *
 * <p>Channel sync must never break core booking flows, so any failure here is
 * swallowed and logged — a missed push is recoverable (manual resync or the
 * next change), an aborted check-out is not.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationChannelListener {

    private final ChannelSyncService channelSync;

    @EventListener
    public void onCreated(ReservationCreatedEvent e) {
        sync(e.getRoomId(), e.getCheckInDate(), e.getCheckOutDate());
    }

    @EventListener
    public void onCancelled(ReservationCancelledEvent e) {
        sync(e.getRoomId(), e.getCheckInDate(), e.getCheckOutDate());
    }

    @EventListener
    public void onCheckedOut(ReservationCheckedOutEvent e) {
        sync(e.getRoomId(), e.getCheckInDate(), e.getCheckOutDate());
    }

    private void sync(RoomId roomId, LocalDate from, LocalDate to) {
        if (from == null || to == null) return;   // legacy events without dates
        try {
            channelSync.syncRoomNights(roomId, from, to);
        } catch (Exception ex) {
            log.warn("Channel availability sync failed for room {} [{}..{}): {}",
                    roomId.value(), from, to, ex.getMessage());
        }
    }
}
