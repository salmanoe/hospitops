package id.co.hospitops.identity.infrastructure.security;

import id.co.hospitops.identity.domain.port.out.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges Spring Security's {@link UserDetailsService} contract with the
 * {@link StaffRepository} domain port.
 *
 * <p>Registering this bean causes Spring Boot's
 * {@code UserDetailsServiceAutoConfiguration} to back off, eliminating the
 * generated-password warning without needing an explicit autoconfiguration
 * exclusion.
 */
@Service
@RequiredArgsConstructor
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String username) {
        return staffRepository.findByUsername(username)
                .map(StaffUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Staff not found: " + username));
    }
}
