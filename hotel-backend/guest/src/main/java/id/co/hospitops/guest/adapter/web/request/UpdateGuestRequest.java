package id.co.hospitops.guest.adapter.web.request;

import jakarta.validation.constraints.*;

public record UpdateGuestRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 100) String nationality,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        String address
) {
}
