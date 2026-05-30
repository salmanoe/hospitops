package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

@Getter
public class ReservationCheckedOutEvent extends DomainEvent {
    private final HotelId hotelId;
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;
    private final long nights;

    public ReservationCheckedOutEvent(HotelId hotelId,
                                      ReservationId reservationId,
                                      RoomId roomId, GuestId guestId, long nights) {
        super();
        this.hotelId = hotelId;
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
        this.nights = nights;
    }
}
