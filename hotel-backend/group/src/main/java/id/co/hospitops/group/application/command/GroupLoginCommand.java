package id.co.hospitops.group.application.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GroupLoginCommand(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
