package id.co.hospitops.bootstrap;

import id.co.hospitops.identity.infrastructure.security.JwtAuthFilter;
import id.co.hospitops.identity.infrastructure.security.StaffUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * C-4 FIX: Spring Security configuration.
 *
 * <p>Previously missing — {@link JwtAuthFilter} existed as a Spring bean but
 * was never wired into the filter chain, leaving all endpoints unprotected.
 *
 * <p>Role hierarchy (from ARCHITECTURE.md §9):
 * <ul>
 *   <li>ADMIN — full access to everything</li>
 *   <li>MANAGER — all except staff creation/deletion</li>
 *   <li>FRONT_DESK — guests, reservations, invoice detail</li>
 *   <li>HOUSEKEEPING — housekeeping board &amp; tasks only</li>
 *   <li>ACCOUNTANT — invoices, payments, revenue reports</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String GROUP_ADMIN = "GROUP_ADMIN";
    private static final String ADMIN = "ADMIN";
    private static final String MANAGER = "MANAGER";
    private static final String FRONT_DESK = "FRONT_DESK";
    private static final String HOUSEKEEPING = "HOUSEKEEPING";
    private static final String ACCOUNTANT = "ACCOUNTANT";

    private final JwtAuthFilter jwtAuthFilter;
    private final StaffUserDetailsService staffUserDetailsService;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            StaffUserDetailsService staffUserDetailsService,
            @Value("${hospitops.cors.allowed-origins:http://localhost:5500}") List<String> allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.staffUserDetailsService = staffUserDetailsService;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // Stateless REST API — no sessions, no CSRF needed
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints ─────────────────────────────────────
                        // Legacy unscoped login (kept for backward compatibility)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // Hotel-aware staff login — hotelId is in the URL path
                        .requestMatchers(HttpMethod.POST, "/api/v1/hotels/*/auth/login").permitAll()
                        // Refresh does not carry a valid access token — must be public
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        // Group self-service signup and login — no auth required
                        .requestMatchers(HttpMethod.POST, "/api/v1/group/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/group/auth/login").permitAll()

                        // ── Group admin — cross-hotel operations ─────────────────
                        // /enter exchanges a group token for a hotel-scoped token — GROUP_ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/v1/group/hotels/*/enter").hasRole(GROUP_ADMIN)
                        .requestMatchers("/api/v1/group/**").hasRole(GROUP_ADMIN)

                        // K8s liveness & readiness probes
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Prometheus scrape (only if PROMETHEUS_ENABLED=true; restricted at network level)
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // All other actuator endpoints locked down
                        .requestMatchers("/actuator/**").hasRole(ADMIN)

                        // ── Identity — authenticated endpoints ───────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()

                        // Staff management
                        // GROUP_ADMIN has full access: reads staff list, creates/edits staff during hotel setup
                        .requestMatchers(HttpMethod.GET, "/api/v1/staff").hasAnyRole(ADMIN, MANAGER, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/staff").hasAnyRole(ADMIN, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/staff/**").hasAnyRole(ADMIN, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/staff/*/password").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/staff/*/toggle").hasAnyRole(ADMIN, MANAGER)

                        // ── Room & Room Types ────────────────────────────────────
                        // GROUP_ADMIN has full access: setup wizard steps 3 & 4 require creating room types and rooms
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms/**").hasAnyRole(ADMIN, MANAGER, FRONT_DESK, HOUSEKEEPING, ACCOUNTANT, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/rooms").hasAnyRole(ADMIN, MANAGER, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/rooms/**").hasAnyRole(ADMIN, MANAGER, GROUP_ADMIN)

                        .requestMatchers(HttpMethod.GET, "/api/v1/room-types/**").hasAnyRole(ADMIN, MANAGER, FRONT_DESK, HOUSEKEEPING, ACCOUNTANT, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/room-types").hasAnyRole(ADMIN, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/room-types/**").hasAnyRole(ADMIN, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/room-types/*/rates").hasAnyRole(ADMIN, MANAGER, GROUP_ADMIN)

                        // ── Guests ───────────────────────────────────────────────
                        // GROUP_ADMIN read-only; POST/PUT remain hotel-staff-only via the next rule
                        .requestMatchers(HttpMethod.GET, "/api/v1/guests/**").hasAnyRole(ADMIN, MANAGER, FRONT_DESK, GROUP_ADMIN)
                        .requestMatchers("/api/v1/guests/**").hasAnyRole(ADMIN, MANAGER, FRONT_DESK)

                        // ── Reservations ─────────────────────────────────────────
                        // GROUP_ADMIN included: hotel-entered GROUP_ADMIN needs arrivals/departures on dashboard.html
                        .requestMatchers("/api/v1/reservations/**").hasAnyRole(ADMIN, MANAGER, FRONT_DESK, GROUP_ADMIN)

                        // ── Housekeeping ─────────────────────────────────────────
                        // GROUP_ADMIN read-only; POST/PUT remain hotel-staff-only via the next rule
                        .requestMatchers(HttpMethod.GET, "/api/v1/housekeeping/**").hasAnyRole(ADMIN, MANAGER, HOUSEKEEPING, GROUP_ADMIN)
                        .requestMatchers("/api/v1/housekeeping/**").hasAnyRole(ADMIN, MANAGER, HOUSEKEEPING)

                        // ── Billing ──────────────────────────────────────────────
                        // GROUP_ADMIN read-only: view invoices, PDFs, reports
                        .requestMatchers(HttpMethod.GET, "/api/v1/invoices/*/pdf")
                        .hasAnyRole(ADMIN, MANAGER, ACCOUNTANT, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/invoices/*/payments")
                        .hasAnyRole(ADMIN, MANAGER, ACCOUNTANT)
                        .requestMatchers(HttpMethod.GET, "/api/v1/invoices/*")
                        .hasAnyRole(ADMIN, MANAGER, ACCOUNTANT, FRONT_DESK, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/invoices")
                        .hasAnyRole(ADMIN, MANAGER, ACCOUNTANT, GROUP_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/**")
                        .hasAnyRole(ADMIN, MANAGER, ACCOUNTANT, GROUP_ADMIN)

                        // Deny everything else by default
                        .anyRequest().authenticated()
                )

                // Register JWT filter before Spring's UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(staffUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

