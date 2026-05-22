package id.co.hospitops.identity.application.command;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
        @NotBlank String username,
        @NotBlank String password
) {
}
