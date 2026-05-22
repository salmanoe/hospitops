package id.co.hospitops.reservation.adapter.web.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * W-8 FIX: Moved to {@code adapter/web/request/} sub-package per INSTRUCTIONS.md.
 * Request DTO classes belong in this sub-package, not directly under {@code adapter/web/}.
 */
public record CreateReservationRequest(
        @NotNull UUID guestId,
        @NotNull UUID roomId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @Min(1) int adults,
        @Min(0) int children,
        String specialRequests
) {
}
