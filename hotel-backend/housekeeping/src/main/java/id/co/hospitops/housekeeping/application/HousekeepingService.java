package id.co.hospitops.housekeeping.application;

import id.co.hospitops.housekeeping.application.response.HousekeepingTaskResponse;
import id.co.hospitops.housekeeping.application.response.RoomStatusEntry;
import id.co.hospitops.housekeeping.application.response.RoomStatusResponse;
import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import id.co.hospitops.housekeeping.domain.port.in.HousekeepingUseCase;
import id.co.hospitops.housekeeping.domain.port.out.HousekeepingTaskRepository;
import id.co.hospitops.housekeeping.domain.port.out.RoomStatusPort;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class HousekeepingService implements HousekeepingUseCase {

    private final HousekeepingTaskRepository taskRepo;
    private final RoomStatusPort roomStatusPort;

    @Override
    public HousekeepingTaskResponse assignTask(UUID taskId, StaffId staffId) {
        HousekeepingTask task = findTask(taskId);
        task.assign(staffId);
        return HousekeepingTaskResponse.from(taskRepo.save(task));
    }

    @Override
    public HousekeepingTaskResponse completeTask(UUID taskId) {
        HousekeepingTask task = findTask(taskId);
        task.complete();
        return HousekeepingTaskResponse.from(taskRepo.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomStatusResponse> getBoardByFloor() {
        // TreeMap preserves ascending floor order without a sort step
        return roomStatusPort.getAllRoomsGroupedByFloor().stream()
                .collect(Collectors.groupingBy(RoomStatusPort.RoomBoardEntry::floor,
                        TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new RoomStatusResponse(e.getKey(),
                        e.getValue().stream()
                                .map(r -> new RoomStatusEntry(r.roomId(), r.roomNumber(),
                                        r.status(), r.roomTypeName()))
                                .sorted(Comparator.comparing(RoomStatusEntry::roomNumber))
                                .toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<HousekeepingTaskResponse> findPendingTasks(Pageable pageable) {
        List<HousekeepingTaskResponse> list = taskRepo.findPending(pageable)
                .stream().map(HousekeepingTaskResponse::from).toList();
        return PageResult.of(list, pageable.getPageNumber(),
                pageable.getPageSize(), taskRepo.countPending());
    }

    @Override
    public HousekeepingTaskResponse createManualTask(RoomId roomId, String notes) {
        HousekeepingTask task = HousekeepingTask.createManual(roomId, notes);
        return HousekeepingTaskResponse.from(taskRepo.save(task));
    }

    @Override
    public void updateRoomStatus(UUID roomId, String status, String notes) {
        roomStatusPort.updateRoomStatus(RoomId.of(roomId), status, notes);
    }

    /**
     * Called by the checkout event listener.
     */
    @Transactional
    public HousekeepingTask createCheckoutTask(RoomId roomId, ReservationId reservationId) {
        HousekeepingTask task = HousekeepingTask.createForCheckout(roomId, reservationId);
        return taskRepo.save(task);
    }

    private HousekeepingTask findTask(UUID id) {
        return taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HousekeepingTask", id));
    }
}
