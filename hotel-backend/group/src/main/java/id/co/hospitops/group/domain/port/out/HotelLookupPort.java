package id.co.hospitops.group.domain.port.out;

import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

/**
 * Outbound port — hotel status queries for the group module.
 *
 * <p>Allows the group module to ask questions about hotels without depending on the
 * hotel module's domain types or JPA layer. Implemented in {@code bootstrap}.
 */
public interface HotelLookupPort {

    /**
     * Returns {@code true} if the hotel exists and is in {@code ACTIVE} status.
     */
    boolean isActive(HotelId hotelId);

    /**
     * Returns {@code true} if the hotel with the given ID belongs to the specified group.
     * Returns {@code false} for unknown hotel IDs.
     */
    boolean belongsToGroup(HotelId hotelId, GroupId groupId);
}
