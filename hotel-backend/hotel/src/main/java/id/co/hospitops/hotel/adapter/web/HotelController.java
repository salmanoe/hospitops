package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.adapter.web.request.CreateHotelRequest;
import id.co.hospitops.hotel.application.command.CompleteSetupStepCommand;
import id.co.hospitops.hotel.application.command.CreateHotelCommand;
import id.co.hospitops.hotel.application.response.HotelResponse;
import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.hotel.domain.port.in.ManageHotelUseCase;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/group/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final ManageHotelUseCase hotelUseCase;

    /**
     * Create a hotel within the authenticated GROUP_ADMIN's group.
     * The hotel starts in SETUP status.
     *
     * <p>The {@code groupId} is derived exclusively from the JWT claim —
     * it is not accepted from the request body to prevent IDOR attacks where a
     * caller creates a hotel under a different group's ID.
     *
     * <p>Requires GROUP_ADMIN role (enforced in SecurityConfig).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HotelResponse>> create(
            @AuthenticationPrincipal GroupAdminPrincipal admin,
            @Valid @RequestBody CreateHotelRequest req) {
        var cmd = new CreateHotelCommand(admin.groupId(), req.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(hotelUseCase.createHotel(cmd)));
    }

    /**
     * List all hotels belonging to the authenticated GROUP_ADMIN's own group.
     * The groupId is derived from the JWT claim — a caller cannot list another group's hotels.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelResponse>>> list(
            @AuthenticationPrincipal GroupAdminPrincipal admin) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.findByGroupId(admin.groupId())));
    }

    /**
     * Get a single hotel by ID, scoped to the authenticated GROUP_ADMIN's group.
     *
     * <p>The groupId is derived from the JWT claim — the service verifies the hotel
     * belongs to that group before returning data, preventing cross-group info disclosure.
     */
    @GetMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<HotelResponse>> get(
            @AuthenticationPrincipal GroupAdminPrincipal admin,
            @PathVariable UUID hotelId) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.findById(HotelId.of(hotelId), admin.groupId())));
    }

    /**
     * Mark a setup wizard step as complete.
     * If all five steps are now complete, the hotel automatically transitions to ACTIVE.
     *
     * <p>The groupId is taken from the JWT claim — prevents a GROUP_ADMIN from completing
     * steps on a hotel belonging to another group.
     */
    @PostMapping("/{hotelId}/setup/{step}")
    public ResponseEntity<ApiResponse<HotelResponse>> completeSetupStep(
            @AuthenticationPrincipal GroupAdminPrincipal admin,
            @PathVariable UUID hotelId,
            @PathVariable SetupStep step) {
        var cmd = new CompleteSetupStepCommand(admin.groupId(), HotelId.of(hotelId), step);
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.completeSetupStep(cmd)));
    }

    /**
     * Suspend an active hotel.
     *
     * <p>The groupId is taken from the JWT claim — prevents a GROUP_ADMIN from suspending
     * a hotel that belongs to another group.
     */
    @PostMapping("/{hotelId}/suspend")
    public ResponseEntity<ApiResponse<HotelResponse>> suspend(
            @AuthenticationPrincipal GroupAdminPrincipal admin,
            @PathVariable UUID hotelId) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.suspend(HotelId.of(hotelId), admin.groupId())));
    }

    /**
     * Reactivate a suspended hotel.
     *
     * <p>The groupId is taken from the JWT claim — prevents a GROUP_ADMIN from reactivating
     * a hotel that belongs to another group.
     */
    @PostMapping("/{hotelId}/reactivate")
    public ResponseEntity<ApiResponse<HotelResponse>> reactivate(
            @AuthenticationPrincipal GroupAdminPrincipal admin,
            @PathVariable UUID hotelId) {
        return ResponseEntity.ok(
                ApiResponse.ok(hotelUseCase.reactivate(HotelId.of(hotelId), admin.groupId())));
    }

    /**
     * Permanently delete a hotel in SETUP status.
     *
     * <p>Only SETUP hotels may be deleted — hotels that have gone ACTIVE contain
     * operational data (reservations, invoices, guests) and must be suspended instead.
     * All hotel-scoped child rows are removed via DB CASCADE.
     *
     * <p>The groupId is taken from the JWT claim to prevent cross-group deletion.
     */
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal GroupAdminPrincipal admin,
            @PathVariable UUID hotelId) {
        hotelUseCase.deleteHotel(HotelId.of(hotelId), admin.groupId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
