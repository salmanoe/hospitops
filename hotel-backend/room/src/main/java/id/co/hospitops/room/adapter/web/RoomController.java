package id.co.hospitops.room.adapter.web;

import id.co.hospitops.room.application.command.*;
import id.co.hospitops.room.application.response.*;
import id.co.hospitops.room.domain.port.in.*;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.web.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RequiresHotelContext
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final ManageRoomUseCase manageUseCase;
    private final RoomAvailabilityUseCase availabilityUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FRONT_DESK','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<PageResult<RoomResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(manageUseCase.findAll(status, PageRequest.of(page, size, Sort.by("roomNumber")))));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FRONT_DESK','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<List<AvailableRoomResponse>>> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(ApiResponse.ok(availabilityUseCase.findAvailable(checkIn, checkOut)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FRONT_DESK','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(manageUseCase.findById(RoomId.of(id))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','GROUP_ADMIN')")
    @SuppressWarnings("JvmTaintAnalysis")
    // @Valid enforces Bean Validation on all string inputs; response is JSON, not HTML
    public ResponseEntity<ApiResponse<RoomResponse>> create(@Valid @RequestBody CreateRoomRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                manageUseCase.createRoom(new CreateRoomCommand(req.roomNumber(), req.floor(), RoomTypeId.of(req.roomTypeId()), req.notes()))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoomRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(manageUseCase.updateRoom(RoomId.of(id), new UpdateRoomCommand(req.floor(), req.notes()))));
    }
}
