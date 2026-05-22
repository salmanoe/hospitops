package id.co.hospitops.housekeeping.application.response;

import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;

import java.time.LocalDateTime;
import java.util.UUID;

public record HousekeepingTaskResponse(
        UUID id,
        RoomId roomId,
        ReservationId reservationId,
        StaffId assignedTo,
        String notes,
        boolean completed,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static HousekeepingTaskResponse from(HousekeepingTask t) {
        return new HousekeepingTaskResponse(
                t.getId(), t.getRoomId(), t.getReservationId(), t.getAssignedTo(),
                t.getNotes(), t.isCompleted(), t.getCompletedAt(), t.getCreatedAt());
    }
}
