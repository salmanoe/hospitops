package id.co.hospitops.reservation.application.response;

import id.co.hospitops.reservation.domain.model.*;
import id.co.hospitops.shared.*;

import java.math.BigDecimal;
import java.time.*;

// R-16 FIX: Added guestFullName and roomNumber for display-layer convenience.
// The basic from(Reservation) factory leaves them null — callers that have
// enriched data (e.g. arrival/departure lists) should use the overloaded
// from(Reservation, String, String) factory instead.
public record ReservationResponse(
        ReservationId id,
        String reservationNumber,
        GuestId guestId,
        RoomId roomId,
        // Display name fetched from the guest module — null if not enriched
        String guestFullName,
        // Room number fetched from the room module — null if not enriched
        String roomNumber,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status,
        BigDecimal ratePerNight,
        BigDecimal subtotal,
        int adults,
        int children,
        long nights,
        String specialRequests,
        LocalDateTime createdAt
) {
    /**
     * Basic factory — guestFullName and roomNumber are not populated.
     */
    public static ReservationResponse from(Reservation r) {
        return from(r, null, null);
    }

    /**
     * Enriched factory — pass guest and room display names for UI consumption.
     */
    public static ReservationResponse from(Reservation r,
                                           String guestFullName,
                                           String roomNumber) {
        return new ReservationResponse(
                r.getId(), r.getReservationNumber(), r.getGuestId(), r.getRoomId(),
                guestFullName, roomNumber,
                r.getCheckInDate(), r.getCheckOutDate(), r.getStatus(),
                r.getRatePerNight().amount(), r.calculateSubtotal().amount(),
                r.getAdults(), r.getChildren(), r.getNights(),
                r.getSpecialRequests(), r.getCreatedAt()
        );
    }
}
