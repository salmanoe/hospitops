package id.co.hospitops.shared.channel;

import id.co.hospitops.shared.RoomTypeId;

import java.time.LocalDate;

/**
 * Everything {@link OtaBookingPort} needs to create a reservation from an OTA
 * booking. Guest fields may be partial — treat blanks defensively.
 */
public record OtaBookingRequest(
        RoomTypeId roomTypeId,
        LocalDate checkIn,
        LocalDate checkOut,
        int adults,
        int children,
        String guestFullName,
        String guestEmail,
        String guestPhone,
        String guestNationality,
        String specialRequests) {
}
