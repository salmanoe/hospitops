package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.adapter.web.request.SavePolicyConfigRequest;
import id.co.hospitops.hotel.application.command.SavePolicyConfigCommand;
import id.co.hospitops.hotel.application.response.PolicyConfigResponse;
import id.co.hospitops.hotel.domain.port.in.ManageHotelPolicyUseCase;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * GROUP_ADMIN endpoints for managing a hotel's policy configuration.
 *
 * <p>Policy config stores the tax rate and invoice branding used when
 * generating invoices for guests of this hotel. Saving a policy config
 * automatically marks the {@code POLICY} setup wizard step complete.
 *
 * <p>Routes are under {@code /api/v1/group/hotels/**}, which is locked to
 * {@code GROUP_ADMIN} role by {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/group/hotels/{hotelId}/policy")
@RequiredArgsConstructor
public class HotelPolicyController {

    private final ManageHotelPolicyUseCase policyUseCase;

    /**
     * Creates or updates the policy configuration for a hotel.
     * Idempotent — calling this multiple times with updated values is safe.
     * Automatically marks the {@code POLICY} setup step complete if the hotel
     * is still in {@code SETUP} status.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<PolicyConfigResponse>> save(
            @PathVariable UUID hotelId,
            @Valid @RequestBody SavePolicyConfigRequest req) {

        var cmd = new SavePolicyConfigCommand(
                HotelId.of(hotelId),
                req.taxPercent(), req.taxName(),
                req.invoiceHotelName(), req.invoiceAddress(), req.invoiceFooterNote());

        return ResponseEntity.ok(ApiResponse.ok(policyUseCase.savePolicyConfig(cmd)));
    }

    /**
     * Returns the current policy configuration for a hotel.
     *
     * @return 200 with the config, or 404 if no policy has been saved yet
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PolicyConfigResponse>> get(
            @PathVariable UUID hotelId) {

        return ResponseEntity.ok(
                ApiResponse.ok(policyUseCase.findByHotelId(HotelId.of(hotelId))));
    }
}
