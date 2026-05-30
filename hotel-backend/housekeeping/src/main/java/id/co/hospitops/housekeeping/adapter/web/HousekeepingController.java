package id.co.hospitops.housekeeping.adapter.web;

// R-04 FIX: Added @Valid to createTask() and assign() so that Jakarta Bean
// Validation is enforced on the request bodies. Without @Valid the @NotNull
// constraints on CreateTaskRequest and AssignTaskRequest are silently ignored,
// allowing null roomId / null staffId to propagate into the service layer and
// produce an opaque NullPointerException instead of a clear 400 response.

import id.co.hospitops.housekeeping.application.response.HousekeepingTaskResponse;
import id.co.hospitops.housekeeping.application.response.RoomStatusResponse;
import id.co.hospitops.housekeeping.domain.port.in.HousekeepingUseCase;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.web.ApiResponse;
import id.co.hospitops.shared.web.PageResult;
import id.co.hospitops.shared.web.RequiresHotelContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiresHotelContext
@RestController
@RequestMapping("/api/v1/housekeeping")
@RequiredArgsConstructor
public class HousekeepingController {

    private final HousekeepingUseCase housekeepingUseCase;

    @GetMapping("/board")
    public ResponseEntity<ApiResponse<List<RoomStatusResponse>>> board() {
        return ResponseEntity.ok(ApiResponse.ok(housekeepingUseCase.getBoardByFloor()));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<PageResult<HousekeepingTaskResponse>>> tasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                housekeepingUseCase.findPendingTasks(PageRequest.of(page, size))));
    }

    // R-04 FIX: @Valid added — was missing, so @NotNull on CreateTaskRequest.roomId
    // was never evaluated and null roomIds reached HousekeepingService.createManualTask().
    @PostMapping("/tasks")
    @SuppressWarnings("JvmTaintAnalysis")
    // @Valid enforces Bean Validation on all string inputs; response is JSON, not HTML
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> createTask(
            @Valid @RequestBody CreateTaskRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        housekeepingUseCase.createManualTask(RoomId.of(req.roomId()), req.notes())));
    }

    // R-04 FIX: @Valid added — was missing, so @NotNull on AssignTaskRequest.staffId
    // was never evaluated, allowing null staffId to reach HousekeepingService.assignTask().
    @PatchMapping("/tasks/{id}/assign")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTaskRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                housekeepingUseCase.assignTask(id, StaffId.of(req.staffId()))));
    }

    @PatchMapping("/tasks/{id}/complete")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> complete(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(housekeepingUseCase.completeTask(id)));
    }

    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<Void> updateRoomStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomStatusRequest req) {
        housekeepingUseCase.updateRoomStatus(id, req.status(), req.notes());
        return ResponseEntity.noContent().build();
    }
}
