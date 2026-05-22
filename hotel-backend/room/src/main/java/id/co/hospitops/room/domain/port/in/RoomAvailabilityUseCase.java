package id.co.hospitops.room.domain.port.in;

import id.co.hospitops.room.application.response.AvailableRoomResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Port for the room availability search endpoint.
 * Returns rooms bookable for the given date range with their effective rate.
 */
public interface RoomAvailabilityUseCase {

    List<AvailableRoomResponse> findAvailable(LocalDate checkIn, LocalDate checkOut);
}
