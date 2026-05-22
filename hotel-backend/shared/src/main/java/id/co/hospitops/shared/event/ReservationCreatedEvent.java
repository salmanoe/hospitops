package id.co.hospitops.shared.event;

import id.co.hospitops.shared.*;

import java.time.LocalDate;

public class ReservationCreatedEvent extends DomainEvent {
    private final ReservationId reservationId;
    private final RoomId roomId;
    private final GuestId guestId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public ReservationCreatedEvent(Object source, ReservationId reservationId,
                                   RoomId roomId, GuestId guestId,
                                   LocalDate checkInDate, LocalDate checkOutDate) {
        super(source);
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.guestId = guestId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
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

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }
}
