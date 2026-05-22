package id.co.hospitops.billing.domain.port.out;

import id.co.hospitops.shared.*;

import java.time.LocalDate;

/**
 * Cross-module port: billing → reservation/room/guest.
 * Retrieves all reservation details needed to generate an invoice without
 * creating compile-time dependencies on other domain models.
 */
public interface ReservationDetailPort {
    ReservationDetail findById(ReservationId id);

    record ReservationDetail(
            ReservationId reservationId,
            String reservationNumber,
            GuestId guestId,
            RoomId roomId,
            String guestFullName,
            String guestIdNumber,
            String guestPhone,
            String roomNumber,
            String roomTypeName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            long nights,
            Money ratePerNight
    ) {
    }
}
