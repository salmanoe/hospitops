package id.co.hospitops.channel.domain.port.out;

import id.co.hospitops.channel.domain.model.ChannelSyncMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence port for the outbox. {@link #save} runs in the enqueuing hotel's
 * context; {@link #findProcessable} is deliberately global (cross-hotel) because
 * the background relay runs with no {@code HotelContext} bound.
 */
public interface ChannelSyncMessageRepository {

    ChannelSyncMessage save(ChannelSyncMessage message);

    /**
     * Oldest PENDING messages whose retry time has arrived, across all hotels,
     * up to {@code limit}. Used by the relay.
     */
    List<ChannelSyncMessage> findProcessable(LocalDateTime now, int limit);

    /** Recent messages for the current hotel, newest first (sync-status board). */
    List<ChannelSyncMessage> findRecentForCurrentHotel(int limit);
}
