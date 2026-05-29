package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.adapter.web.request.CreateHotelRequest;
import id.co.hospitops.hotel.application.command.CompleteSetupStepCommand;
import id.co.hospitops.hotel.application.command.CreateHotelCommand;
import id.co.hospitops.hotel.application.response.HotelResponse;
import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.hotel.domain.port.in.ManageHotelUseCase;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/group/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final ManageHotelUseCase hotelUseCase;

    /**
     * Create a hotel within a group. The hotel starts in SETUP status.
     * Requires GROUP_ADMIN role (enforced in SecurityConfig).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HotelResponse>> create(
            @Valid @RequestBody CreateHotelRequest req) {
        var cmd = new CreateHotelCommand(GroupId.of(req.groupId()), req.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(hotelUseCase.createHotel(cmd)));
    }

    /**
     * List all hotels belonging to a group.
     *
     * TODO Phase 6: Extract groupId from JWT claim, remove query parameter.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelResponse>>> list(
            @RequestParam UUID groupId) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.findByGroupId(GroupId.of(groupId))));
    }

    /** Get a single hotel by ID. */
    @GetMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<HotelResponse>> get(
            @PathVariable UUID hotelId) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.findById(HotelId.of(hotelId))));
    }

    /**
     * Mark a setup wizard step as complete.
     * If all five steps are now complete, the hotel automatically transitions to ACTIVE.
     */
    @PostMapping("/{hotelId}/setup/{step}")
    public ResponseEntity<ApiResponse<HotelResponse>> completeSetupStep(
            @PathVariable UUID hotelId,
            @PathVariable SetupStep step) {
        var cmd = new CompleteSetupStepCommand(HotelId.of(hotelId), step);
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.completeSetupStep(cmd)));
    }

    /** Suspend an active hotel. */
    @PostMapping("/{hotelId}/suspend")
    public ResponseEntity<ApiResponse<HotelResponse>> suspend(
            @PathVariable UUID hotelId) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.suspend(HotelId.of(hotelId))));
    }

    /** Reactivate a suspended hotel. */
    @PostMapping("/{hotelId}/reactivate")
    public ResponseEntity<ApiResponse<HotelResponse>> reactivate(
            @PathVariable UUID hotelId) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.reactivate(HotelId.of(hotelId))));
    }
}
