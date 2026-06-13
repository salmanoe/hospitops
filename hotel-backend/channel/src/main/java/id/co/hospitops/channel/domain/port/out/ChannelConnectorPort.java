package id.co.hospitops.channel.domain.port.out;

import id.co.hospitops.channel.domain.model.AriUpdate;
import id.co.hospitops.channel.domain.model.BookingRevision;

import java.util.List;

/**
 * Outbound port to the channel-connectivity provider. Implemented by the
 * Channex adapter. Called by the outbox relay, not directly by request
 * handlers, so failures are retried rather than surfaced to the user.
 */
public interface ChannelConnectorPort {

    /**
     * Push availability + rates for one property.
     *
     * @throws ChannelConnectorException on any non-success provider response
     *                                   or transport error, so the relay retries.
     */
    void pushAri(String externalPropertyId, List<AriUpdate> updates);

    /**
     * Fetch one page of the inbound booking-revisions feed (unacked, oldest
     * first, across all properties the key can see). Empty when nothing is due.
     *
     * @throws ChannelConnectorException on transport/provider error.
     */
    List<BookingRevision> fetchRevisionFeed();

    /** Acknowledge a processed revision so the provider stops re-serving it. */
    void ackRevision(String revisionId);
}

