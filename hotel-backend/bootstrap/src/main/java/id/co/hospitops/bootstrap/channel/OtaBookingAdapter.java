package id.co.hospitops.bootstrap.channel;

import id.co.hospitops.guest.application.command.RegisterGuestCommand;
import id.co.hospitops.guest.domain.port.in.ManageGuestUseCase;
import id.co.hospitops.reservation.application.command.CreateReservationCommand;
import id.co.hospitops.reservation.application.response.ReservationResponse;
import id.co.hospitops.reservation.domain.port.in.ReservationUseCase;
import id.co.hospitops.room.application.response.AvailableRoomResponse;
import id.co.hospitops.room.domain.port.in.RoomAvailabilityUseCase;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.channel.OtaBookingPort;
import id.co.hospitops.shared.channel.OtaBookingRequest;
import id.co.hospitops.shared.channel.OtaBookingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Composition-root implementation of {@link OtaBookingPort}: orchestrates the
 * room, guest and reservation modules to turn an inbound OTA booking into a
 * HospitOps reservation. Lives in {@code bootstrap} because it is the only
 * module that may depend on all bounded contexts.
 *
 * <p>Runs inside the hotel context bound by the channel inbound processor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtaBookingAdapter implements OtaBookingPort {

    private final RoomAvailabilityUseCase roomAvailability;
    private final ManageGuestUseCase guests;
    private final ReservationUseCase reservations;

    @Override
    public Optional<OtaBookingResult> createBooking(OtaBookingRequest req) {
        // Channex sells room types; assign a concrete free room of that type.
        Optional<RoomId> room = roomAvailability.findAvailable(req.checkIn(), req.checkOut()).stream()
                .filter(r -> r.roomTypeId().equals(req.roomTypeId()))
                .map(AvailableRoomResponse::id)
                .findFirst();
        if (room.isEmpty()) {
            return Optional.empty();   // overbooking — no room of this type free
        }

        GuestId guestId = resolveGuest(req);
        ReservationResponse res = reservations.create(new CreateReservationCommand(
                guestId, room.get(), req.checkIn(), req.checkOut(),
                Math.max(1, req.adults()), Math.max(0, req.children()),
                req.specialRequests(),
                null /* OTA booking — no staff actor; created_by is nullable */));
        return Optional.of(new OtaBookingResult(res.id(), res.reservationNumber()));
    }

    @Override
    public void cancelBooking(ReservationId reservationId) {
        reservations.cancel(reservationId);
    }

    /** Reuse an existing guest matched by email, otherwise register a new one. */
    private GuestId resolveGuest(OtaBookingRequest req) {
        String email = req.guestEmail();
        if (email != null && !email.isBlank()) {
            Optional<GuestId> existing = guests.search(email, PageRequest.of(0, 1)).content().stream()
                    .filter(g -> email.equalsIgnoreCase(g.email()))
                    .map(g -> g.id())
                    .findFirst();
            if (existing.isPresent()) return existing.get();
        }
        return guests.register(new RegisterGuestCommand(
                guestName(req.guestFullName()),
                null,
                blankToNull(req.guestNationality()),
                blankToNull(req.guestPhone()),
                blankToNull(email),
                null)).id();
    }

    private static String guestName(String name) {
        return (name == null || name.isBlank()) ? "OTA Guest" : name;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
