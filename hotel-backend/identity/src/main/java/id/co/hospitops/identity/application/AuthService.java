package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.LoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.port.in.AuthUseCase;
import id.co.hospitops.identity.domain.port.out.RefreshTokenStore;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.identity.domain.port.out.TokenService;
import id.co.hospitops.identity.infrastructure.security.StaffUserDetails;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class AuthService implements AuthUseCase {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final TokenBlacklist tokenBlacklist;
    private final RefreshTokenStore refreshTokenStore;
    private final StaffRepository staffRepository;
    private final long refreshExpirationSeconds;

    public AuthService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            TokenBlacklist tokenBlacklist,
            RefreshTokenStore refreshTokenStore,
            StaffRepository staffRepository,
            @Value("${hospitops.refresh-token.expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.tokenBlacklist = tokenBlacklist;
        this.refreshTokenStore = refreshTokenStore;
        this.staffRepository = staffRepository;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginCommand command) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(command.username(), command.password())
            );
            Staff staff = ((StaffUserDetails) Objects.requireNonNull(authentication.getPrincipal())).staff();

            String accessToken  = tokenService.generate(staff);
            String refreshToken = issueRefreshToken(staff.getId());

            log.info("Staff {} logged in", staff.getUsername());
            return LoginResponse.of(accessToken, tokenService.getExpirationSeconds(),
                    refreshToken, refreshExpirationSeconds, staff);

        } catch (DisabledException e) {
            throw new BusinessRuleViolationException("Account is deactivated");
        } catch (BadCredentialsException e) {
            throw new BusinessRuleViolationException("Invalid username or password");
        }
    }

    /**
     * Validates the supplied refresh token, loads the owning staff member, and
     * issues a fresh access + refresh token pair. The old refresh token is
     * atomically revoked (rotation) so each refresh token is single-use.
     *
     * @throws BusinessRuleViolationException if the token is unknown, expired, or
     *                                        the owning staff member is inactive
     */
    @Override
    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        StaffId staffId = refreshTokenStore.findStaffId(refreshToken)
                .orElseThrow(() -> new BusinessRuleViolationException("Refresh token is invalid or expired"));

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId.value()));

        if (!staff.isActive()) {
            throw new BusinessRuleViolationException("Account is deactivated");
        }

        // Rotate: revoke the old token before issuing the new one.
        // If the store revocation fails the old token should not be accepted again
        // because findStaffId will return empty after the TTL elapses anyway.
        refreshTokenStore.revoke(refreshToken);
        String newRefreshToken = issueRefreshToken(staffId);
        String newAccessToken  = tokenService.generate(staff);

        log.info("Tokens refreshed for staff {}", staff.getUsername());
        return LoginResponse.of(newAccessToken, tokenService.getExpirationSeconds(),
                newRefreshToken, refreshExpirationSeconds, staff);
    }

    /**
     * Blacklists the access token and, if supplied, also revokes the refresh token.
     *
     * @param accessToken  the Bearer JWT to blacklist — must not be null
     * @param refreshToken the opaque refresh token to revoke, or {@code null} to
     *                     skip refresh revocation
     */
    @Override
    public void logout(String accessToken, String refreshToken) {
        tokenBlacklist.invalidate(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.revoke(refreshToken);
        }
        log.info("Logout complete — access token blacklisted, refresh token revoked={}", refreshToken != null);
    }

    private String issueRefreshToken(StaffId staffId) {
        String token = UUID.randomUUID().toString();
        refreshTokenStore.store(token, staffId, refreshExpirationSeconds);
        return token;
    }
}
