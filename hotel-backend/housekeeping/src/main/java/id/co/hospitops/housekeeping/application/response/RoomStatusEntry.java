package id.co.hospitops.housekeeping.application.response;

import id.co.hospitops.shared.RoomId;

public record RoomStatusEntry(
        RoomId roomId,
        String roomNumber,
        String status,
        String roomTypeName
) {
}
