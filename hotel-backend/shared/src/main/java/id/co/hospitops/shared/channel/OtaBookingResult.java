package id.co.hospitops.shared.channel;

import id.co.hospitops.shared.ReservationId;

public record OtaBookingResult(ReservationId reservationId, String reservationNumber) {
}
