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
     * Updates a room's housekeeping status (AVAILABLE, MAINTENANCE).
     * Called by {@code PATCH /api/v1/housekeeping/rooms/{id}/status}.
     */
    void updateRoomStatus(UUID roomId, String status, String notes);
}
