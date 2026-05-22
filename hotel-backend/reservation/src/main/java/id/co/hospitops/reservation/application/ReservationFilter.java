package id.co.hospitops.reservation.application;

import id.co.hospitops.reservation.domain.model.ReservationStatus;

import java.util.Optional;

/**
 * Filter Object pattern — replaces nullable String statusFilter.
 * ReservationFilter.all()            -> no status filter
 * ReservationFilter.byStatus(CONFIRMED) -> filter by status
 */
public record ReservationFilter(Optional<ReservationStatus> status) {

    public static ReservationFilter all() {
        return new ReservationFilter(Optional.empty());
    }

    public static ReservationFilter byStatus(ReservationStatus status) {
        return new ReservationFilter(Optional.of(status));
    }

    public static ReservationFilter fromString(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return all();
        return byStatus(ReservationStatus.valueOf(statusStr.toUpperCase()));
    }

    public boolean hasStatusFilter() {
        return status.isPresent();
    }
}
