package id.co.hospitops.channel.adapter.web;

import id.co.hospitops.channel.application.command.ConfigureChannelPropertyCommand;
import id.co.hospitops.channel.application.command.MapRoomTypeCommand;
import id.co.hospitops.channel.application.command.PushAriCommand;
import id.co.hospitops.channel.application.response.ChannelPropertyMappingResponse;
import id.co.hospitops.channel.application.response.ChannelRoomTypeMappingResponse;
import id.co.hospitops.channel.domain.port.in.ManageChannelMappingUseCase;
import id.co.hospitops.channel.domain.port.in.SyncChannelUseCase;
import id.co.hospitops.shared.RoomTypeId;
import id.co.hospitops.shared.web.ApiResponse;
import id.co.hospitops.shared.web.RequiresHotelContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Channel mapping configuration for the current hotel — how HospitOps is
 * wired onto the channel provider (Channex) before ARI sync runs. Hotel-scoped.
 */
@RequiresHotelContext
@RestController
@RequestMapping("/api/v1/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ManageChannelMappingUseCase useCase;
    private final SyncChannelUseCase syncUseCase;

    // ── Property hookup ─────────────────────────────────────────────────

    @GetMapping("/property")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<ChannelPropertyMappingResponse>> getProperty() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getProperty()));
    }

    @PutMapping("/property")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<ChannelPropertyMappingResponse>> configureProperty(
            @Valid @RequestBody ConfigureChannelPropertyRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                useCase.configureProperty(new ConfigureChannelPropertyCommand(req.externalPropertyId()))));
    }

    @PostMapping("/property/enable")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<ChannelPropertyMappingResponse>> enable() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.enableChannel()));
    }

    @PostMapping("/property/disable")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<ChannelPropertyMappingResponse>> disable() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.disableChannel()));
    }

    // ── Room-type mappings ──────────────────────────────────────────────

    @GetMapping("/room-types")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<List<ChannelRoomTypeMappingResponse>>> listRoomTypeMappings() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.listRoomTypeMappings()));
    }

    @PutMapping("/room-types/{roomTypeId}")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<ChannelRoomTypeMappingResponse>> mapRoomType(
            @PathVariable UUID roomTypeId, @Valid @RequestBody MapRoomTypeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.mapRoomType(new MapRoomTypeCommand(
                RoomTypeId.of(roomTypeId), req.externalRoomTypeId(), req.externalRatePlanId()))));
    }

    // ── Outbound ARI ────────────────────────────────────────────────────

    /**
     * Enqueue an availability/rate push for a room type. Delivery to the
     * provider is asynchronous (outbox relay), so this returns 202 Accepted.
     */
    @PostMapping("/room-types/{roomTypeId}/push")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','GROUP_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> pushAri(
            @PathVariable UUID roomTypeId, @Valid @RequestBody PushAriRequest req) {
        syncUseCase.enqueueAriPush(new PushAriCommand(
                RoomTypeId.of(roomTypeId),
                req.nights().stream()
                        .map(n -> new PushAriCommand.Night(n.date(), n.availability(), n.rate()))
                        .toList()));
        return ResponseEntity.accepted().body(
                ApiResponse.<Void>ok("Channel sync enqueued", null));
    }
}
