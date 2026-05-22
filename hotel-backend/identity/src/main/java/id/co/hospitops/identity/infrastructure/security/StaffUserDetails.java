package id.co.hospitops.identity.infrastructure.security;

import id.co.hospitops.identity.domain.model.Staff;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security adapter over the {@link Staff} domain model.
 *
 * <p>Keeps the domain model free of Spring Security coupling by wrapping it
 * here in the infrastructure layer. The {@link Staff} instance is retained so
 * callers can unwrap it when they need domain-specific fields (e.g. in
 * {@link id.co.hospitops.identity.infrastructure.security.JwtAuthFilter}).
 */
public record StaffUserDetails(Staff staff) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + staff.getRole().name()));
    }

    @Override
    public String getPassword() {
        return staff.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return staff.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return staff.isActive();
    }
}
