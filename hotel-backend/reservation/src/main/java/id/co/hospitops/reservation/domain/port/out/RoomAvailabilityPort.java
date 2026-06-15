package id.co.hospitops.reservation.domain.port.out;

import id.co.hospitops.shared.*;

import java.time.LocalDate;

public interface RoomAvailabilityPort {
    boolean isAvailable(RoomId roomId, LocalDate checkIn, LocalDate checkOut);

    Money resolveRate(RoomId roomId, LocalDate checkIn);

    /** Total rooms in the current hotel — the basis for available room-nights. */
    long totalRooms();
}
