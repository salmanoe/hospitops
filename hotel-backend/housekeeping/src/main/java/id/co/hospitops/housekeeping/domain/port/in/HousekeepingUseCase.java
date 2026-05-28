package id.co.hospitops.housekeeping.domain.port.in;

import id.co.hospitops.housekeeping.application.response.HousekeepingTaskResponse;
import id.co.hospitops.housekeeping.application.response.RoomStatusResponse;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.web.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface HousekeepingUseCase {
    HousekeepingTaskResponse assignTask(UUID taskId, StaffId staffId);

    HousekeepingTaskResponse completeTask(UUID taskId);

    List<RoomStatusResponse> getBoardByFloor();

    PageResult<HousekeepingTaskResponse> findPendingTasks(Pageable pageable);

    HousekeepingTaskResponse createManualTask(RoomId roomId, String notes);

    /**
     * Updates a room's housekeeping status.
     * Permitted transitions via this endpoint:
     * <ul>
     *   <li>DIRTY / MAINTENANCE → AVAILABLE (housekeeping complete, post-checkout or repairs)</li>
     *   <li>Any → MAINTENANCE (take room out of service; blocked while guest is present)</li>
     *   <li>OCCUPIED → SERVICE_REQUESTED (guest requests mid-stay cleaning)</li>
     *   <li>SERVICE_REQUESTED → OCCUPIED (housekeeping completes mid-stay service)</li>
     * </ul>
     * Called by {@code PATCH /api/v1/housekeeping/rooms/{id}/status}.
     */
    void updateRoomStatus(UUID roomId, String status, String notes);
}
