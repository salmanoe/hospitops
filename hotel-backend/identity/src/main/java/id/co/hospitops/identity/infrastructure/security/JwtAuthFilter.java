package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

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

        // Extract hotelId before proceeding — determines whether to bind HotelContext
        HotelId hotelId = null;
        try {
            DecodedJWT decoded = jwtUtil.parse(token);
            StaffId staffId = StaffId.of(UUID.fromString(decoded.getSubject()));
            String role = decoded.getClaim("role").asString();

            staffRepository.findById(staffId).ifPresent(staff -> {
                if (!staff.isActive()) return;

                var auth = new UsernamePasswordAuthenticationToken(
                        staff, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });

            String hotelIdStr = decoded.getClaim("hotelId").asString();
            if (hotelIdStr != null && !hotelIdStr.isBlank()) {
                hotelId = HotelId.of(UUID.fromString(hotelIdStr));
            }
        } catch (Exception e) {
            log.warn("JWT processing error: {}", e.getMessage());
        }

        // Bind HotelContext for the remainder of the filter chain.
        // GROUP_ADMIN requests without a hotelId claim proceed without a hotel context —
        // hotel-scoped endpoints are protected by SecurityConfig and will reject them.
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
}
