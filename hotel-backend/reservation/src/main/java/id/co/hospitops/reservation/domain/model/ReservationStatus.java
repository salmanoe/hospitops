package id.co.hospitops.reservation.domain.model;

import org.hibernate.annotations.Struct;

@Struct(name = "reservation_status")
public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    public boolean canCheckIn() {
        return this == CONFIRMED;
    }

    public boolean canCheckOut() {
        return this == CHECKED_IN;
    }

    public boolean canCancel() {
        return this == PENDING || this == CONFIRMED;
    }
}
