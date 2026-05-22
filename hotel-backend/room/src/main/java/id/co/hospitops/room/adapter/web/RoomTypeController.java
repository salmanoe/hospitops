package id.co.hospitops.room.adapter.web;
import id.co.hospitops.room.application.command.*;
import id.co.hospitops.room.application.response.*;
import id.co.hospitops.room.domain.port.in.ManageRoomTypeUseCase;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.web.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/room-types") @RequiredArgsConstructor
public class RoomTypeController {
    private final ManageRoomTypeUseCase useCase;

    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FRONT_DESK','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResult<RoomTypeResponse>>> list(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.findAllRoomTypes(PageRequest.of(page, size, Sort.by("name")))));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FRONT_DESK')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.findRoomTypeById(RoomTypeId.of(id))));
    }

    @PostMapping @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> create(@Valid @RequestBody CreateRoomTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
            useCase.createRoomType(new CreateRoomTypeCommand(req.name(), req.capacity(), req.description(), req.basePrice()))));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoomTypeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
            useCase.updateRoomType(RoomTypeId.of(id), new UpdateRoomTypeCommand(req.name(), req.capacity(), req.description(), req.basePrice()))));
    }

    @PostMapping("/{id}/rates") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> addRate(@PathVariable UUID id, @Valid @RequestBody AddRateOverrideRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
            useCase.addRateOverride(RoomTypeId.of(id), new AddRateOverrideCommand(req.name(), req.priceOverride(), req.validFrom(), req.validUntil()))));
    }
}
