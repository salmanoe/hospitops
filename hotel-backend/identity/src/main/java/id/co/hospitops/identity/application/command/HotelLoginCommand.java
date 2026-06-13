package id.co.hospitops.identity.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command for the hotel-aware staff login endpoint.
 *
 * <p>The {@code hotelId} comes from the URL path — it is set by the controller,
 * not from the request body.
 */
public record HotelLoginCommand(
        @NotNull UUID hotelId,
        @NotBlank String username,
        @NotBlank String password
) {
}
