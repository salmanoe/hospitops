package id.co.hospitops.guest.application.command;

import jakarta.validation.constraints.*;

public record RegisterGuestCommand(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 50) String idNumber,
        @Size(max = 100) String nationality,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        String address
) {
}
