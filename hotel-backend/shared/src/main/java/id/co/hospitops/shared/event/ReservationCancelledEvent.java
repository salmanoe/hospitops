package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class ReservationCancelledEvent extends DomainEvent {
    private final HotelId hotelId;
    private final ReservationId reservationId;
    private final RoomId roomId;

    public ReservationCancelledEvent(HotelId hotelId,
                                     ReservationId reservationId,
                                     RoomId roomId) {
        super();
        this.hotelId = hotelId;
        this.reservationId = reservationId;
        this.roomId = roomId;
    }
}
