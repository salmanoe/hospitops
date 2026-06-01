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

    /**
     * Combined check used by the {@code /enter} endpoint.
     *
     * <p>Returns a result encoding both the group-membership check and the active-status
     * check in a single database round-trip. Callers should use this instead of calling
     * {@link #belongsToGroup} and {@link #isActive} separately.
     */
    HotelAccessResult verifyAccess(HotelId hotelId, GroupId groupId);

    /** Encodes the outcome of a combined hotel-access check. */
    enum HotelAccessResult {
        /** Hotel does not exist, or does not belong to the caller's group. */
        NOT_FOUND_OR_WRONG_GROUP,
        /** Hotel belongs to the group but is SUSPENDED — entry blocked. */
        SUSPENDED,
        /**
         * Hotel belongs to the group and is ACTIVE or SETUP — entry permitted.
         * SETUP hotels must be enterable so GROUP_ADMIN can complete the setup wizard.
         */
        ALLOWED
    }
}
