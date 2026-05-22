package id.co.hospitops.reservation.application.command;

import id.co.hospitops.shared.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateReservationCommand(
        @NotNull GuestId guestId,
        @NotNull RoomId roomId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @Min(1) int adults,
        @Min(0) int children,
        String specialRequests,
        @NotNull StaffId createdBy
) {
}
