package id.co.hospitops.reservation.domain.model;

import org.hibernate.annotations.Struct;

@Struct(name = "reservation_status")
public enum ReservationStatus {
    /**
     * Retained for legacy data only. {@code Reservation.create()} always produces
     * {@code CONFIRMED}; the DB default was aligned to {@code CONFIRMED} in V7 migration
     * (R-06 fix). New reservations are never created with this status.
     */
    PENDING,
    CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

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
