package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class ReservationCheckedInEvent extends DomainEvent {
    private final HotelId hotelId;
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;

    public ReservationCheckedInEvent(HotelId hotelId,
                                     ReservationId reservationId,
                                     RoomId roomId, GuestId guestId) {
        super();
        this.hotelId = hotelId;
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
    }
}
