package id.co.hospitops.channel.domain.port.out;

import id.co.hospitops.channel.domain.model.AriUpdate;

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
}
