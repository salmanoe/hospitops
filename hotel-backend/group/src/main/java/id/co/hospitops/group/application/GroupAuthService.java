package id.co.hospitops.group.application;

import id.co.hospitops.group.application.command.GroupLoginCommand;
import id.co.hospitops.group.application.response.GroupLoginResponse;
import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.group.domain.port.in.GroupAuthUseCase;
import id.co.hospitops.group.domain.port.out.GroupAdminRepository;
import id.co.hospitops.group.domain.port.out.GroupTokenService;
import id.co.hospitops.group.domain.port.out.HotelLookupPort;
import id.co.hospitops.group.domain.port.out.HotelLookupPort.HotelAccessResult;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * GROUP_ADMIN authentication and hotel context-switching.
 *
 * <p>Login deliberately mirrors the credential-check pattern in {@code HotelAuthService}:
 * no {@code AuthenticationManager} involvement, direct password comparison.
 * This keeps GROUP_ADMIN auth fully independent of Spring Security's staff-oriented
 * {@code UserDetailsService}.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupAuthService implements GroupAuthUseCase {

    private final GroupAdminRepository adminRepository;
    private final GroupTokenService tokenService;
    private final HotelLookupPort hotelLookupPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public GroupLoginResponse login(GroupLoginCommand command) {
        GroupAdmin admin = adminRepository.findByEmail(command.email())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid email or password"));

        if (!passwordEncoder.matches(command.password(), admin.getPasswordHash())) {
            throw new BusinessRuleViolationException("Invalid email or password");
        }

        String token = tokenService.issueGroupToken(admin.getId(), admin.getGroupId(), admin.getEmail());
        log.info("GROUP_ADMIN {} logged in", admin.getEmail());

        return GroupLoginResponse.groupScoped(token, tokenService.getExpirationSeconds(),
                admin.getId(), admin.getGroupId(), admin.getEmail());
    }

    @Override
    public GroupLoginResponse enterHotel(GroupAdminPrincipal admin, HotelId targetHotel,
                                         String groupToken) {
        // Combined single-query check: group membership + active status.
        HotelAccessResult access = hotelLookupPort.verifyAccess(targetHotel, admin.groupId());
        if (access == HotelAccessResult.NOT_FOUND_OR_WRONG_GROUP) {
            throw new BusinessRuleViolationException("Hotel does not belong to your group");
        }
        if (access == HotelAccessResult.SUSPENDED) {
            throw new BusinessRuleViolationException("Hotel is suspended and cannot be entered");
        }

        // Rule 3: hotel-scoped token carries the same expiry as the group token
        Instant parentExpiry = tokenService.parseExpiry(groupToken);
        String hotelToken = tokenService.issueHotelToken(
                admin.id(), admin.groupId(), admin.email(), targetHotel, parentExpiry);

        String hotelName = hotelLookupPort.findHotelName(targetHotel);
        log.info("GROUP_ADMIN {} entered hotel {}", admin.email(), targetHotel.value());
        return GroupLoginResponse.hotelScoped(hotelToken, tokenService.getExpirationSeconds(),
                admin.id(), admin.groupId(), admin.email(), targetHotel, hotelName);
    }
}
