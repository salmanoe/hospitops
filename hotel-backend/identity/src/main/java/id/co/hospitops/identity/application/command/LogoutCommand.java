package id.co.hospitops.identity.application.command;

/**
 * Optional request body for {@code POST /api/v1/auth/logout}.
 *
 * <p>When provided, the refresh token is also revoked so the client cannot use
 * it to obtain a new access token after logout. If omitted, only the current
 * access token is blacklisted.
 *
 * @param refreshToken the opaque refresh token to revoke, or {@code null}
 */
public record LogoutCommand(String refreshToken) {
}
