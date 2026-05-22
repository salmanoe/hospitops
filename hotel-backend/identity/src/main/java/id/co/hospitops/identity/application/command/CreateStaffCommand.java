package id.co.hospitops.identity.application.command;

import id.co.hospitops.identity.domain.model.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStaffCommand(
        @NotBlank String fullName,
        @NotBlank String username,
        @NotBlank String password,
        @NotNull StaffRole role
) {
}
