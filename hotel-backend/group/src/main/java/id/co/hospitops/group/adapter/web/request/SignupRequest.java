package id.co.hospitops.group.adapter.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "Group name is required")
        @Size(max = 200, message = "Group name must not exceed 200 characters")
        String groupName,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String adminEmail,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {}
