package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;

public class ReservationCancelledEvent extends DomainEvent {
    private final ReservationId reservationId;
    private final RoomId roomId;

    public ReservationCancelledEvent(Object source, ReservationId reservationId,
                                     RoomId roomId) {
        super(source);
        this.reservationId = reservationId;
        this.roomId = roomId;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public RoomId getRoomId() {
        return roomId;
    }
}
