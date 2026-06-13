package id.co.hospitops.channel.infrastructure.event;

import id.co.hospitops.channel.application.ChannelSyncService;
import id.co.hospitops.shared.event.RateChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Pushes a room type's new rate to the channel manager when it changes.
 * Runs in the rate-edit transaction (outbox write is atomic with the override).
 * Failures are swallowed so channel sync never blocks a price change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateChangeChannelListener {

    private final ChannelSyncService channelSync;

    @EventListener
    public void onRateChanged(RateChangedEvent e) {
        if (e.getFrom() == null || e.getTo() == null) return;
        try {
            // RateChangedEvent dates are inclusive; syncRoomTypeNights end is exclusive.
            channelSync.syncRoomTypeNights(e.getRoomTypeId(), e.getFrom(), e.getTo().plusDays(1));
        } catch (Exception ex) {
            log.warn("Channel rate sync failed for room type {} [{}..{}]: {}",
                    e.getRoomTypeId().value(), e.getFrom(), e.getTo(), ex.getMessage());
        }
    }
}
