package id.co.hospitops.hotel.adapter.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHotelRequest(

        @NotBlank(message = "Hotel name is required")
        @Size(max = 200, message = "Hotel name must not exceed 200 characters")
        String name
) {}
