package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ReservationCreatedEvent extends DomainEvent {
    private final HotelId hotelId;
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public ReservationCreatedEvent(HotelId hotelId,
                                   ReservationId reservationId,
                                   RoomId roomId, GuestId guestId,
                                   LocalDate checkInDate, LocalDate checkOutDate) {
        super();
        this.hotelId = hotelId;
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }
}
