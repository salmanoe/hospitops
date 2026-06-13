package id.co.hospitops.shared.channel;

import id.co.hospitops.shared.ReservationId;

import java.util.Optional;

/**
 * Shared-kernel SPI for turning an inbound OTA booking into a HospitOps
 * reservation, implemented in the composition root ({@code bootstrap}) which
 * can orchestrate the guest, room and reservation modules. The channel module
 * depends only on this interface.
 *
 * <p>Called within a bound {@code HotelContext} (the inbound processor resolves
 * the hotel from the OTA property first).
 */
public interface OtaBookingPort {

    /**
     * Find-or-create the guest, assign an available room of the requested type,
     * and create a reservation. Returns empty when no room of that type is free
     * for the dates — the overbooking case the caller must surface, not crash on.
     */
    Optional<OtaBookingResult> createBooking(OtaBookingRequest request);

    /** Cancel a previously created OTA reservation. */
    void cancelBooking(ReservationId reservationId);
}
