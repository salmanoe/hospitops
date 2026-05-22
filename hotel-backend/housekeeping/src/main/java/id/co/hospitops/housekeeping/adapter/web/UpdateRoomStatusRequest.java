package id.co.hospitops.housekeeping.adapter.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for PATCH /housekeeping/rooms/{id}/status (I7 fix).
 */
public record UpdateRoomStatusRequest(
        @NotBlank String status,
        String notes
) {
}
