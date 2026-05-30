package id.co.hospitops.identity.domain.port.out;

import id.co.hospitops.shared.HotelId;

/**
 * Outbound port — queries hotel status without coupling the identity module
 * to the hotel module's JPA layer.
 *
 * <p>Implemented in {@code bootstrap} using {@code HotelJpaRepository}.
 */
public interface HotelStatusPort {

    /**
     * Returns {@code true} if the hotel exists and is in {@code ACTIVE} status.
     * Returns {@code false} for hotels in {@code SETUP} or {@code SUSPENDED} status,
     * and for unknown hotel IDs.
     */
    boolean isActive(HotelId hotelId);
}
