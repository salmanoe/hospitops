package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.LoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.port.in.AuthUseCase;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.identity.domain.port.out.TokenService;
import id.co.hospitops.identity.infrastructure.security.StaffUserDetails;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final TokenBlacklist tokenBlacklist;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginCommand command) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(command.username(), command.password())
            );
            Staff staff = ((StaffUserDetails) Objects.requireNonNull(authentication.getPrincipal())).staff();
            String token = tokenService.generate(staff);
            long expiresIn = tokenService.getExpirationSeconds();
            return LoginResponse.of(token, expiresIn, staff);
        } catch (DisabledException e) {
            throw new BusinessRuleViolationException("Account is deactivated");
        } catch (BadCredentialsException e) {
            throw new BusinessRuleViolationException("Invalid username or password");
        }
    }

    @Override
    public void logout(String token) {
        tokenBlacklist.invalidate(token);
    }
}
