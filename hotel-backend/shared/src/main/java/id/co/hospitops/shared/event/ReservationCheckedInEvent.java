package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class ReservationCheckedInEvent extends DomainEvent {
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;

    public ReservationCheckedInEvent(ReservationId reservationId,
                                     RoomId roomId, GuestId guestId) {
        super();
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
    }
}
