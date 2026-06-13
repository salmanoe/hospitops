package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ReservationCheckedOutEvent extends DomainEvent {
    private final HotelId hotelId;
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;
    private final long nights;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public ReservationCheckedOutEvent(HotelId hotelId,
                                      ReservationId reservationId,
                                      RoomId roomId, GuestId guestId, long nights,
                                      LocalDate checkInDate, LocalDate checkOutDate) {
        super();
        this.hotelId = hotelId;
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
        this.nights = nights;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }

    /**
     * @deprecated use {@link #ReservationCheckedOutEvent(HotelId, ReservationId, RoomId, GuestId, long, LocalDate, LocalDate)}
     * — the stay dates are needed to sync freed availability to the channel manager.
     */
    @Deprecated
    public ReservationCheckedOutEvent(HotelId hotelId, ReservationId reservationId,
                                      RoomId roomId, GuestId guestId, long nights) {
        this(hotelId, reservationId, roomId, guestId, nights, null, null);
    }
}
