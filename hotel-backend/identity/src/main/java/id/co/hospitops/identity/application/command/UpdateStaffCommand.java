package id.co.hospitops.identity.application.command;

import id.co.hospitops.identity.domain.model.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffCommand(
        @NotBlank String fullName,
        @NotNull StaffRole role
) {
}
