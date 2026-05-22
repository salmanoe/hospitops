package id.co.hospitops.housekeeping.domain.model;

import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class HousekeepingTask {

    private final UUID id;
    private final RoomId roomId;
    private final ReservationId reservationId;  // nullable — task trigger
    private StaffId assignedTo;
    private String notes;
    private boolean completed;
    private LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Created automatically on checkout.
     */
    public static HousekeepingTask createForCheckout(RoomId roomId,
                                                     ReservationId reservationId) {
        return new HousekeepingTask(UUID.randomUUID(), roomId, reservationId,
                null, "Room requires cleaning after checkout",
                false, null, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Created manually by manager / housekeeping staff.
     */
    public static HousekeepingTask createManual(RoomId roomId, String notes) {
        return new HousekeepingTask(UUID.randomUUID(), roomId, null,
                null, notes, false, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static HousekeepingTask reconstitute(UUID id, RoomId roomId,
                                                ReservationId reservationId,
                                                StaffId assignedTo, String notes,
                                                boolean completed, LocalDateTime completedAt,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new HousekeepingTask(id, roomId, reservationId, assignedTo,
                notes, completed, completedAt, createdAt, updatedAt);
    }

    private HousekeepingTask(UUID id, RoomId roomId, ReservationId reservationId,
                             StaffId assignedTo, String notes, boolean completed,
                             LocalDateTime completedAt, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.roomId = roomId;
        this.reservationId = reservationId;
        this.assignedTo = assignedTo;
        this.notes = notes;
        this.completed = completed;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void assign(StaffId staffId) {
        this.assignedTo = staffId;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.completed)
            throw new IllegalStateException("Task already completed");
        this.completed = true;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }
}
