package id.co.hospitops.group.domain.port.out;

import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

import java.time.Instant;

/**
 * Outbound port — GROUP_ADMIN token issuance and parsing.
 *
 * <p>Implemented in {@code bootstrap} using {@code JwtUtil} from the {@code identity} module.
 * The group module is intentionally decoupled from JWT internals.
 */
public interface GroupTokenService {

    /**
     * Issues a group-scoped GROUP_ADMIN token (no {@code hotelId} claim).
     * Applies the configured default TTL.
     */
    String issueGroupToken(GroupAdminId adminId, GroupId groupId, String email);

    /**
     * Issues a hotel-scoped GROUP_ADMIN token.
     *
     * @param expiresAt the exact expiry instant to embed — must equal the originating
     *                  group token's expiry so the hotel token cannot outlive the group session
     */
    String issueHotelToken(GroupAdminId adminId, GroupId groupId, String email,
                           HotelId hotelId, Instant expiresAt);

    /**
     * Parses the expiry instant from a signed token string.
     * Used by the {@code /enter} endpoint to extract the group token's expiry
     * before issuing a hotel-scoped token with the same TTL.
     *
     * @throws IllegalArgumentException if the token is invalid or unparseable
     */
    Instant parseExpiry(String token);

    /**
     * Returns the configured token lifetime in seconds (for login response).
     */
    long getExpirationSeconds();
}
