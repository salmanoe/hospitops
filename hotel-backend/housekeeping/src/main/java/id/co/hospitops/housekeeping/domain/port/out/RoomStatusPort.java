package id.co.hospitops.housekeeping.domain.port.out;

import id.co.hospitops.shared.RoomId;

import java.util.List;

/**
 * Cross-module port: housekeeping → room.
 * Fetches room status information for the board view without a direct
 * compile-time dependency on the room module's domain model.
 */
public interface RoomStatusPort {
    List<RoomBoardEntry> getAllRoomsGroupedByFloor();

    /**
     * Applies a housekeeping status change to a room.
     * The status string is resolved to a domain transition in the adapter.
     * Allowed values: AVAILABLE, MAINTENANCE.
     */
    void updateRoomStatus(RoomId roomId, String status, String notes);

    record RoomBoardEntry(
            RoomId roomId,
            String roomNumber,
            int floor,
            String status,
            String roomTypeName
    ) {
    }
}
