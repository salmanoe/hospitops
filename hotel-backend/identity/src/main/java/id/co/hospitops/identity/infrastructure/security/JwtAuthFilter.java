package id.co.hospitops.identity.infrastructure.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.shared.StaffId;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
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
        } catch (Exception e) {
            log.warn("JWT processing error: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
