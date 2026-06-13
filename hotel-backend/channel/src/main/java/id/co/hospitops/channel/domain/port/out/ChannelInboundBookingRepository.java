package id.co.hospitops.channel.domain.port.out;

import id.co.hospitops.channel.domain.model.ChannelInboundBooking;

import java.util.Optional;

/**
 * Persistence port for inbound-booking idempotency records. Scoped to the
 * current {@code HotelContext} (the inbound processor binds it after resolving
 * the hotel from the OTA property).
 */
public interface ChannelInboundBookingRepository {

    ChannelInboundBooking save(ChannelInboundBooking booking);

    Optional<ChannelInboundBooking> findByExternalBookingId(String externalBookingId);
}
