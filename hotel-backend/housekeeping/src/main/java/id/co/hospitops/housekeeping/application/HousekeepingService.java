package id.co.hospitops.housekeeping.application;

import id.co.hospitops.housekeeping.application.response.HousekeepingTaskResponse;
import id.co.hospitops.housekeeping.application.response.RoomStatusEntry;
import id.co.hospitops.housekeeping.application.response.RoomStatusResponse;
import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import id.co.hospitops.housekeeping.domain.port.in.HousekeepingUseCase;
import id.co.hospitops.housekeeping.domain.port.out.HousekeepingTaskRepository;
import id.co.hospitops.housekeeping.domain.port.out.RoomStatusPort;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.event.HousekeepingTaskCreatedEvent;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
        HousekeepingTask task = HousekeepingTask.createManual(HotelContext.current(), roomId, notes);
        return HousekeepingTaskResponse.from(taskRepo.save(task));
    }

    @Override
    public void updateRoomStatus(UUID roomId, String status, String notes) {
        RoomId rid = RoomId.of(roomId);
        roomStatusPort.updateRoomStatus(rid, status, notes);
        if ("SERVICE_REQUESTED".equalsIgnoreCase(status)) {
            String taskNotes = (notes != null && !notes.isBlank()) ? notes : "Guest requested cleaning service";
            HousekeepingTask task = HousekeepingTask.createManual(HotelContext.current(), rid, taskNotes);
            taskRepo.save(task);
        }
    }

    /**
     * Called by the checkout event listener. The hotelId is passed explicitly from the
     * event rather than reading HotelContext, because AFTER_COMMIT listeners run after
     * the transaction completes and the ScopedValue binding may no longer be active.
     */
    @Transactional
    public HousekeepingTask createCheckoutTask(HotelId hotelId,
                                               RoomId roomId,
                                               ReservationId reservationId) {
        HousekeepingTask task = HousekeepingTask.createForCheckout(hotelId, roomId, reservationId);
        HousekeepingTask saved = taskRepo.save(task);
        eventPublisher.publishEvent(
                new HousekeepingTaskCreatedEvent(hotelId, saved.getId(), roomId));
        return saved;
    }

    private HousekeepingTask findTask(UUID id) {
        return taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HousekeepingTask", id));
    }
}
