package id.co.hospitops.shared;

import org.jspecify.annotations.Nullable;

import java.security.Principal;

/**
 * Spring Security principal for a GROUP_ADMIN session.
 *
 * <p>Set by {@code JwtAuthFilter} when the verified JWT carries role {@code GROUP_ADMIN}.
 * Unlike hotel staff, GROUP_ADMIN identity is resolved entirely from JWT claims — no
 * database lookup is performed during authentication.
 *
 * <p>{@code hotelId} is non-null only after the token-exchange endpoint
 * ({@code POST /api/v1/group/hotels/{hotelId}/enter}) issues a hotel-scoped token.
 */
public record GroupAdminPrincipal(
        GroupAdminId id,
        GroupId groupId,
        String email,
        @Nullable HotelId hotelId
) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
