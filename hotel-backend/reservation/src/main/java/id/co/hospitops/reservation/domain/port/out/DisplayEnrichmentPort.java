package id.co.hospitops.reservation.domain.port.out;

import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.RoomId;

/**
 * Cross-module port: fetches display names for enriched reservation responses.
 */
public interface DisplayEnrichmentPort {
    GuestDisplay findGuestDisplay(GuestId id);

    RoomDisplay findRoomDisplay(RoomId id);

    record GuestDisplay(String fullName, String idNumber) {
    }

    record RoomDisplay(String roomNumber, String roomTypeName) {
    }
}
