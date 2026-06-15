package id.co.hospitops.room.domain.port.in;

import id.co.hospitops.room.application.command.CreateRoomCommand;
import id.co.hospitops.room.application.command.UpdateRoomCommand;
import id.co.hospitops.room.application.response.RoomResponse;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.web.PageResult;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Primary port for room management and cross-module availability.
 *
 * Admin CRUD methods are used by the room module's own controller.
 * The isAvailable and resolveRate methods are cross-module operations:
 * the reservation module's RoomAvailabilityAdapter calls them so it
 * never imports a concrete infrastructure class.
 */
public interface ManageRoomUseCase {

    // --- Admin CRUD ---

    RoomResponse createRoom(CreateRoomCommand cmd);

    RoomResponse updateRoom(RoomId id, UpdateRoomCommand cmd);

    RoomResponse findById(RoomId id);

    PageResult<RoomResponse> findAll(String statusFilter, Pageable pageable);

    /**
     * Total number of rooms in the current hotel. Used by the reservation
     * module's revenue analytics to compute available room-nights (the
     * denominator of RevPAR and occupancy).
     */
    long countRooms();

    // --- Cross-module operations ---

    /**
     * Returns true if the room is available for the given date range
     * (check-out exclusive). Used by the reservation module.
     */
    boolean isAvailable(RoomId id, LocalDate checkIn, LocalDate checkOut);

    /**
     * Resolves the effective nightly rate for the room on the given date,
     * applying any active price overrides. Used by the reservation module.
     */
    Money resolveRate(RoomId id, LocalDate checkIn);

    /**
     * Changes a room's operational status directly.
     * Allowed targets: AVAILABLE (cleaning complete), MAINTENANCE (out of service).
     * Business-rule guards are enforced by the Room domain model — illegal
     * transitions (e.g. OCCUPIED → MAINTENANCE) throw IllegalStateException.
     * Used by the housekeeping module via RoomStatusAdapter.
     */
    void changeRoomStatus(RoomId id, RoomStatus newStatus, String notes);
}
