package id.co.hospitops.room.domain.model;

import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.RoomTypeId;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Room {

    private final RoomId     id;
    private final String     roomNumber;
    private int              floor;
    private RoomStatus       status;
    private final RoomTypeId roomTypeId;
    private String           notes;
    private final LocalDateTime createdAt;
    private LocalDateTime    updatedAt;

    public static Room create(String roomNumber, int floor,
                              RoomTypeId roomTypeId, String notes) {
        if (roomNumber == null || roomNumber.isBlank())
            throw new IllegalArgumentException("Room number cannot be blank");
        if (floor < 1)
            throw new IllegalArgumentException("Floor must be >= 1");
        return new Room(RoomId.generate(), roomNumber, floor,
                        RoomStatus.AVAILABLE, roomTypeId, notes,
                        LocalDateTime.now(), LocalDateTime.now());
    }

    public static Room reconstitute(RoomId id, String roomNumber, int floor,
                                    RoomStatus status, RoomTypeId roomTypeId,
                                    String notes, LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
        return new Room(id, roomNumber, floor, status,
                        roomTypeId, notes, createdAt, updatedAt);
    }

    private Room(RoomId id, String roomNumber, int floor, RoomStatus status,
                 RoomTypeId roomTypeId, String notes,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.roomNumber = roomNumber; this.floor = floor;
        this.status = status; this.roomTypeId = roomTypeId; this.notes = notes;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    // ── Business rules (State Machine) ──────────────────────────────

    /** Check-in: only AVAILABLE rooms may become OCCUPIED. */
    public void markOccupied() {
        if (this.status != RoomStatus.AVAILABLE)
            throw new IllegalStateException(
                "Room " + roomNumber + " cannot be occupied from state " + status +
                " (must be AVAILABLE)");
        this.status    = RoomStatus.OCCUPIED;
        this.updatedAt = LocalDateTime.now();
    }

    // R-05 FIX: All four status transitions now have explicit source-state guards.
    // Previously markDirty(), markAvailable(), and markMaintenance() had no guards,
    // allowing impossible transitions such as AVAILABLE → DIRTY (before any guest).

    /** Check-out: only OCCUPIED rooms may become DIRTY. */
    public void markDirty() {
        if (this.status != RoomStatus.OCCUPIED)
            throw new IllegalStateException(
                "Room " + roomNumber + " cannot be marked dirty from state " + status +
                " (must be OCCUPIED)");
        this.status    = RoomStatus.DIRTY;
        this.updatedAt = LocalDateTime.now();
    }

    /** Housekeeping complete: only DIRTY rooms may become AVAILABLE. */
    public void markAvailable() {
        if (this.status != RoomStatus.DIRTY && this.status != RoomStatus.MAINTENANCE)
            throw new IllegalStateException(
                "Room " + roomNumber + " cannot be marked available from state " + status +
                " (must be DIRTY or MAINTENANCE)");
        this.status    = RoomStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }

    /** Maintenance: any non-OCCUPIED room may be taken out of service. */
    public void markMaintenance(String reason) {
        if (this.status == RoomStatus.OCCUPIED)
            throw new IllegalStateException(
                "Room " + roomNumber + " cannot be placed in maintenance while OCCUPIED");
        this.status    = RoomStatus.MAINTENANCE;
        this.notes     = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDetails(int floor, String notes) {
        if (floor < 1) throw new IllegalArgumentException("Floor must be >= 1");
        this.floor     = floor;
        this.notes     = notes;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Resolves the effective price for a given check-in date.
     * Checks overrides first; falls back to room type base price.
     * Rate is locked at booking time — see reservation.rate_per_night column.
     */
    public Money resolveRate(LocalDate checkInDate, RoomType roomType,
                             List<RoomRateOverride> overrides) {
        return overrides.stream()
                .filter(o -> o.roomTypeId().equals(this.roomTypeId))
                .filter(o -> o.isActiveOn(checkInDate))
                .findFirst()
                .map(RoomRateOverride::priceOverride)
                .orElse(roomType.getBasePrice());
    }
}
