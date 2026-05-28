package id.co.hospitops.identity.domain.port.in;

import id.co.hospitops.identity.application.command.LoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;

public interface AuthUseCase {

    LoginResponse login(LoginCommand command);

    /**
     * Issues a new access + refresh token pair, rotating (invalidating) the
     * supplied refresh token so it cannot be reused.
     *
     * @param refreshToken the opaque refresh token issued at login or last refresh
     * @return a fresh {@link LoginResponse} with new tokens
     * @throws id.co.hospitops.shared.exception.BusinessRuleViolationException
     *         if the token is unknown, expired, or the owning staff is inactive
     */
    LoginResponse refresh(String refreshToken);

    /**
     * Revokes the given access token and optionally a refresh token.
     *
     * @param accessToken  the Bearer JWT to blacklist
     * @param refreshToken the opaque refresh token to revoke, or {@code null} to
     *                     skip refresh revocation (e.g. when client has already lost it)
     */
    void logout(String accessToken, String refreshToken);
}
