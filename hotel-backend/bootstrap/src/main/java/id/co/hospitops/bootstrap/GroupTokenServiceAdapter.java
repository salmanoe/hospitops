package id.co.hospitops.bootstrap;

import com.auth0.jwt.exceptions.JWTVerificationException;
import id.co.hospitops.group.domain.port.out.GroupTokenService;
import id.co.hospitops.identity.infrastructure.security.JwtUtil;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Implements the {@code group} module's {@link GroupTokenService} using {@code JwtUtil}
 * from the {@code identity} module.
 *
 * <p>Lives in {@code bootstrap} — the only module allowed to depend on both
 * {@code group} and {@code identity} simultaneously.
 */
@Component
@RequiredArgsConstructor
public class GroupTokenServiceAdapter implements GroupTokenService {

    private final JwtUtil jwtUtil;

    @Override
    public String issueGroupToken(GroupAdminId adminId, GroupId groupId, String email) {
        return jwtUtil.generateGroupAdminToken(adminId, groupId, email, null, null);
    }

    @Override
    public String issueHotelToken(GroupAdminId adminId, GroupId groupId, String email,
                                  HotelId hotelId, Instant expiresAt) {
        return jwtUtil.generateGroupAdminToken(adminId, groupId, email, hotelId, expiresAt);
    }

    @Override
    public Instant parseExpiry(String token) {
        try {
            return jwtUtil.parse(token).getExpiresAtAsInstant();
        } catch (JWTVerificationException e) {
            throw new IllegalArgumentException("Cannot parse token expiry: " + e.getMessage(), e);
        }
    }

    @Override
    public long getExpirationSeconds() {
        return jwtUtil.getExpirationSeconds();
    }
}
