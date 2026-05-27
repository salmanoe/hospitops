package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class ReservationCheckedOutEvent extends DomainEvent {
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;
    private final long nights;

    public ReservationCheckedOutEvent(Object source, ReservationId reservationId,
                                      RoomId roomId, GuestId guestId, long nights) {
        super(source);
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
        this.nights = nights;
    }
}
