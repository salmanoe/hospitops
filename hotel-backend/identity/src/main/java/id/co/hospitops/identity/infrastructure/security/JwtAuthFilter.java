package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter — runs once per request.
 *
 * <p>Two token types are handled:
 * <ul>
 *   <li><b>Staff tokens</b> (role ≠ GROUP_ADMIN): the {@code sub} claim is a {@code StaffId}.
 *       The staff record is loaded from the DB to verify it is still active.</li>
 *   <li><b>GROUP_ADMIN tokens</b> (role = GROUP_ADMIN): the {@code sub} claim is a
 *       {@code GroupAdminId}. Identity is resolved entirely from JWT claims — no DB lookup.
 *       This avoids coupling the filter to the {@code group} module's persistence layer.</li>
 * </ul>
 *
 * <p>If a {@code hotelId} claim is present (hotel-scoped token), {@link HotelContext}
 * is bound for the remainder of the filter chain via {@link ScopedValue}.
 * GROUP_ADMIN requests without a {@code hotelId} claim proceed without a hotel context —
 * hotel-scoped endpoints guard themselves with {@code @RequiresHotelContext}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String ROLE_GROUP_ADMIN = "GROUP_ADMIN";

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final StaffRepository staffRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (tokenBlacklist.isBlacklisted(token) || !jwtUtil.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        HotelId hotelId = null;
        try {
            DecodedJWT decoded = jwtUtil.parse(token);
            String role = decoded.getClaim("role").asString();

            String hotelIdStr = decoded.getClaim("hotelId").asString();
            if (hotelIdStr != null && !hotelIdStr.isBlank()) {
                hotelId = HotelId.of(UUID.fromString(hotelIdStr));
            }

            if (ROLE_GROUP_ADMIN.equals(role)) {
                authenticateGroupAdmin(decoded, hotelId, request);
            } else {
                authenticateStaff(decoded, role, request);
            }
        } catch (Exception e) {
            log.warn("JWT processing error: {}", e.getMessage());
        }

        // Bind HotelContext for the remainder of the filter chain.
        // Requests without a hotelId claim proceed without hotel context.
        if (hotelId != null) {
            final HotelId boundHotelId = hotelId;
            try {
                ScopedValue.where(HotelContext.HOTEL_ID, boundHotelId)
                        .call(() -> {
                            chain.doFilter(request, response);
                            return null;
                        });
            } catch (IOException | ServletException e) {
                throw e;
            } catch (Exception e) {
                throw new ServletException("Unexpected error in hotel-context filter scope", e);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Resolves GROUP_ADMIN identity from JWT claims only — no DB lookup.
     * The principal is a {@link GroupAdminPrincipal} carrying the claims extracted from the token.
     */
    private void authenticateGroupAdmin(DecodedJWT decoded, HotelId hotelId,
                                        HttpServletRequest request) {
        GroupAdminId adminId = GroupAdminId.of(UUID.fromString(decoded.getSubject()));
        GroupId groupId = GroupId.of(UUID.fromString(decoded.getClaim("groupId").asString()));
        String email = decoded.getClaim("email").asString();

        var principal = new GroupAdminPrincipal(adminId, groupId, email, hotelId);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_GROUP_ADMIN"))
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Resolves hotel staff identity. Loads the staff record from DB to verify
     * the account is still active — a deactivated staff member is not authenticated.
     */
    private void authenticateStaff(DecodedJWT decoded, String role, HttpServletRequest request) {
        StaffId staffId = StaffId.of(UUID.fromString(decoded.getSubject()));

        staffRepository.findById(staffId).ifPresent(staff -> {
            if (!staff.isActive()) return;

            var auth = new UsernamePasswordAuthenticationToken(
                    staff, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        });
    }
}
