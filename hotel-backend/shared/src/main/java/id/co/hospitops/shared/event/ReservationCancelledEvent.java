package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ReservationCancelledEvent extends DomainEvent {
    private final HotelId hotelId;
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public ReservationCancelledEvent(HotelId hotelId,
                                     ReservationId reservationId,
                                     RoomId roomId,
                                     LocalDate checkInDate,
                                     LocalDate checkOutDate) {
        super();
        this.hotelId = hotelId;
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }

    /**
     * @deprecated use {@link #ReservationCancelledEvent(HotelId, ReservationId, RoomId, LocalDate, LocalDate)}
     * — the stay dates are needed to sync freed availability to the channel manager.
     */
    @Deprecated
    public ReservationCancelledEvent(HotelId hotelId, ReservationId reservationId, RoomId roomId) {
        this(hotelId, reservationId, roomId, null, null);
    }
}
