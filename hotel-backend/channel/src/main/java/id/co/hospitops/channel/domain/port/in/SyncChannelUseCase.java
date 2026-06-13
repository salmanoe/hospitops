package id.co.hospitops.channel.domain.port.in;

import id.co.hospitops.channel.application.command.PushAriCommand;

/**
 * Enqueues channel-sync work for the current hotel. The actual delivery to the
 * provider happens asynchronously via the outbox relay.
 */
public interface SyncChannelUseCase {

    /**
     * Resolve the room type's provider mapping, build an ARI message and enqueue
     * it for delivery. Used by the manual "resync" endpoint and (Slice 2b) by
     * the reservation/rate event listeners.
     */
    void enqueueAriPush(PushAriCommand command);
}
