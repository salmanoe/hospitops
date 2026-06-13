package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.HotelLoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.port.in.HotelAuthUseCase;
import id.co.hospitops.identity.domain.port.out.HotelStatusPort;
import id.co.hospitops.identity.domain.port.out.RefreshTokenStore;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenService;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Hotel-aware staff authentication.
 *
 * <p>Unlike {@link AuthService}, this service does NOT use Spring's
 * {@code AuthenticationManager} — it performs the credential check directly.
 * This is intentional: {@code AuthenticationManager} relies on
 * {@code StaffUserDetailsService.loadUserByUsername(username)} which is unscoped,
 * and would allow staff from Hotel B to authenticate against Hotel A's login endpoint.
 *
 * <p>The contract here is:
 * <ol>
 *   <li>Hotel must be ACTIVE (guards against SETUP/SUSPENDED hotels)</li>
 *   <li>Staff must exist <em>in this specific hotel</em></li>
 *   <li>Staff must be active (not deactivated)</li>
 *   <li>Password must match</li>
 * </ol>
 *
 * <p>On success, issues a hotel-scoped access + refresh token pair.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class HotelAuthService implements HotelAuthUseCase {

    private final StaffRepository staffRepository;
    private final HotelStatusPort hotelStatusPort;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final long refreshExpirationSeconds;

    public HotelAuthService(
            StaffRepository staffRepository,
            HotelStatusPort hotelStatusPort,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            RefreshTokenStore refreshTokenStore,
            @Value("${hospitops.refresh-token.expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        this.staffRepository = staffRepository;
        this.hotelStatusPort = hotelStatusPort;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenStore = refreshTokenStore;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Override
    public LoginResponse login(HotelLoginCommand command) {
        HotelId hotelId = HotelId.of(command.hotelId());

        // Rule 1: hotel must be ACTIVE — return 403-equivalent before attempting any
        // credential check (avoids timing-based hotel discovery attacks)
        if (!hotelStatusPort.isActive(hotelId)) {
            throw new BusinessRuleViolationException("Hotel is not currently active");
        }

        // Rule 2 + 3: staff must exist in this hotel and be active
        Staff staff = staffRepository.findByUsernameAndHotelId(command.username(), hotelId)
                .filter(Staff::isActive)
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid username or password"));

        // Rule 4: password must match
        if (!passwordEncoder.matches(command.password(), staff.getPasswordHash())) {
            throw new BusinessRuleViolationException("Invalid username or password");
        }

        String accessToken = tokenService.generate(staff);
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenStore.store(refreshToken, staff.getId(), refreshExpirationSeconds);

        log.info("Staff {} logged into hotel {}", staff.getUsername(), hotelId.value());
        return LoginResponse.of(accessToken, tokenService.getExpirationSeconds(),
                refreshToken, refreshExpirationSeconds, staff);
    }
}
