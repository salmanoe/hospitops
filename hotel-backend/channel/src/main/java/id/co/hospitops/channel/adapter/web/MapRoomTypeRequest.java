package id.co.hospitops.channel.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MapRoomTypeRequest(
        @NotBlank @Size(max = 128) String externalRoomTypeId,
        @NotBlank @Size(max = 128) String externalRatePlanId) {
}
