package id.co.hospitops.reservation.domain.model;

import id.co.hospitops.shared.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public class Reservation {

    private final ReservationId id;
    private final String reservationNumber;
    private final GuestId guestId;
    private final RoomId roomId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private ReservationStatus status;
    private final Money ratePerNight;
    private final int adults;
    private final int children;
    private final String specialRequests;
    private final StaffId createdBy;
    private final HotelId hotelId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Reservation create(HotelId hotelId, String reservationNumber, GuestId guestId,
                                     RoomId roomId, LocalDate checkIn, LocalDate checkOut,
                                     Money ratePerNight, int adults, int children,
                                     String specialRequests, StaffId createdBy) {
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn))
            throw new IllegalArgumentException("Check-out must be after check-in");
        if (adults < 1)
            throw new IllegalArgumentException("At least 1 adult required");

        return new Reservation(ReservationId.generate(), reservationNumber,
                guestId, roomId, checkIn, checkOut, ReservationStatus.CONFIRMED,
                ratePerNight, adults, children, specialRequests, createdBy, hotelId,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static Reservation reconstitute(ReservationId id, String number,
                                           GuestId guestId, RoomId roomId,
                                           LocalDate checkIn, LocalDate checkOut,
                                           ReservationStatus status, Money ratePerNight,
                                           int adults, int children, String specialRequests,
                                           StaffId createdBy, HotelId hotelId,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Reservation(id, number, guestId, roomId, checkIn, checkOut,
                status, ratePerNight, adults, children, specialRequests,
                createdBy, hotelId, createdAt, updatedAt);
    }

    private Reservation(ReservationId id, String reservationNumber, GuestId guestId,
                        RoomId roomId, LocalDate checkIn, LocalDate checkOut,
                        ReservationStatus status, Money ratePerNight, int adults,
                        int children, String specialRequests, StaffId createdBy,
                        HotelId hotelId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reservationNumber = reservationNumber;
        this.guestId = guestId;
        this.roomId = roomId;
        this.checkInDate = checkIn;
        this.checkOutDate = checkOut;
        this.status = status;
        this.ratePerNight = ratePerNight;
        this.adults = adults;
        this.children = children;
        this.specialRequests = specialRequests;
        this.createdBy = createdBy;
        this.hotelId = hotelId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void checkIn() {
        if (!status.canCheckIn())
            throw new IllegalStateException("Cannot check in reservation in status: " + status);
        this.status = ReservationStatus.CHECKED_IN;
        this.updatedAt = LocalDateTime.now();
    }

    public void checkOut() {
        if (!status.canCheckOut())
            throw new IllegalStateException("Cannot check out reservation in status: " + status);
        this.status = ReservationStatus.CHECKED_OUT;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (!status.canCancel())
            throw new IllegalStateException("Cannot cancel reservation in status: " + status);
        this.status = ReservationStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public Money calculateSubtotal() {
        return ratePerNight.multiply((int) getNights());
    }

    /**
     * Number of this reservation's nights that fall within the window
     * {@code [from, to)} (check-out exclusive, mirroring how a stay occupies
     * the nights of {@code [checkIn, checkOut)}). Used by revenue analytics so a
     * stay straddling the window edge only contributes its in-window nights.
     */
    public long nightsWithin(LocalDate from, LocalDate to) {
        LocalDate start = checkInDate.isAfter(from) ? checkInDate : from;
        LocalDate end = checkOutDate.isBefore(to) ? checkOutDate : to;
        long nights = ChronoUnit.DAYS.between(start, end);
        return Math.max(0, nights);
    }

    /** Room revenue earned within {@code [from, to)} = in-window nights × nightly rate. */
    public Money roomRevenueWithin(LocalDate from, LocalDate to) {
        return ratePerNight.multiply((int) nightsWithin(from, to));
    }
}
