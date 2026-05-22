package id.co.hospitops.billing.infrastructure.adapter;

import id.co.hospitops.billing.domain.port.out.ReservationDetailPort;
import id.co.hospitops.guest.domain.port.in.ManageGuestUseCase;
import id.co.hospitops.reservation.domain.port.in.ReservationUseCase;
import id.co.hospitops.room.domain.port.in.ManageRoomUseCase;
import id.co.hospitops.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stage 1+2: direct calls. Stage 3: swap to HTTP client only here.
 */
@Component
@RequiredArgsConstructor
public class ReservationDetailAdapter implements ReservationDetailPort {

    private final ReservationUseCase reservationService;
    private final ManageGuestUseCase guestService;
    private final ManageRoomUseCase roomService;

    @Override
    public ReservationDetail findById(ReservationId id) {
        var res = reservationService.findById(id);
        var guest = guestService.findById(res.guestId());
        var room = roomService.findById(res.roomId());
        return new ReservationDetail(
                res.id(), res.reservationNumber(), res.guestId(), res.roomId(),
                guest.fullName(), guest.idNumber(), guest.phone(),
                room.roomNumber(), room.roomTypeName(),
                res.checkInDate(), res.checkOutDate(),
                res.nights(), Money.of(res.ratePerNight())
        );
    }
}
