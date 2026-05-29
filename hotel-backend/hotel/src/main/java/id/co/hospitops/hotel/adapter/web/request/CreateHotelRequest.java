package id.co.hospitops.hotel.adapter.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateHotelRequest(

        /**
         * TODO Phase 6: Remove groupId from the request body.
         * Extract it from the GROUP_ADMIN JWT claim instead.
         */
        @NotNull(message = "Group ID is required")
        UUID groupId,

        @NotBlank(message = "Hotel name is required")
        @Size(max = 200, message = "Hotel name must not exceed 200 characters")
        String name
) {}
