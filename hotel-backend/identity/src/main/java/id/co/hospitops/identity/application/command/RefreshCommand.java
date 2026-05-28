package id.co.hospitops.identity.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/refresh}.
 *
 * @param refreshToken the opaque refresh token issued at login or last refresh
 */
public record RefreshCommand(@NotBlank String refreshToken) {
}
