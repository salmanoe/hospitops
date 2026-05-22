package id.co.hospitops.identity.application.command;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordCommand(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
