package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class ReservationCheckedInEvent extends DomainEvent {
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;

    public ReservationCheckedInEvent(Object source, ReservationId reservationId,
                                     RoomId roomId, GuestId guestId) {
        super(source);
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
    }
}
