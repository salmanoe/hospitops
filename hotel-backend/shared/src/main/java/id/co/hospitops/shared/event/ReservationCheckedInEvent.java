package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;

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

    public ReservationId getReservationId() {
        return reservationId;
    }

    public RoomId getRoomId() {
        return roomId;
    }

    public GuestId getGuestId() {
        return guestId;
    }
}
