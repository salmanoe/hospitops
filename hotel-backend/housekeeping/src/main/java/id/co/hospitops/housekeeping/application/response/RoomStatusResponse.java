package id.co.hospitops.housekeeping.application.response;

import java.util.List;

public record RoomStatusResponse(
        int floor,
        List<RoomStatusEntry> rooms
) {
}
