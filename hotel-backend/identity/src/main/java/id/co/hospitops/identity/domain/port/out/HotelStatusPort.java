package id.co.hospitops.identity.domain.port.out;

import id.co.hospitops.shared.HotelId;

import java.util.Optional;

/**
 * Outbound port — queries hotel status without coupling the identity module
 * to the hotel module's JPA layer.
 *
 * <p>Implemented in {@code bootstrap} using {@code HotelJpaRepository}.
 */
public interface HotelStatusPort {

    /**
     * Returns the hotel's id and name <em>only</em> when it exists and is in
     * {@code ACTIVE} status. Returns {@link Optional#empty()} for hotels in
     * {@code SETUP} or {@code SUSPENDED} status, and for unknown hotel IDs.
     *
     * <p>Used by the staff login flow to (a) guard against logging into a
     * non-active hotel and (b) surface the hotel name in the login response —
     * both resolved in a single lookup.
     */
    Optional<HotelInfo> findActiveHotel(HotelId hotelId);

    /** Minimal hotel identity returned by {@link #findActiveHotel(HotelId)}. */
    record HotelInfo(HotelId id, String name) {
    }
}
