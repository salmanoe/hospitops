package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class ReservationCancelledEvent extends DomainEvent {
    private final ReservationId reservationId;
    private final RoomId roomId;

    public ReservationCancelledEvent(ReservationId reservationId,
                                     RoomId roomId) {
        super();
        this.reservationId = reservationId;
        this.roomId = roomId;
    }
}
