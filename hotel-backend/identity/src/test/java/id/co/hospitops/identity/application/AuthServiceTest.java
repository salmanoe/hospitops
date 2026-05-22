package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.LoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.identity.domain.port.out.TokenService;
import id.co.hospitops.identity.infrastructure.security.StaffUserDetails;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock TokenService tokenService;
    @Mock TokenBlacklist tokenBlacklist;
    @Mock Authentication authentication;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, tokenService, tokenBlacklist);
    }

    private Staff activeStaff() {
        return Staff.reconstitute(StaffId.generate(), "Test User", "testuser",
                "hashed", StaffRole.FRONT_DESK, true, LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns JWT when credentials are valid")
        void returnsJwtForValidCredentials() {
            Staff staff = activeStaff();
            given(authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken("testuser", "correctpass")))
                    .willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(new StaffUserDetails(staff));
            given(tokenService.generate(staff)).willReturn("jwt-token-abc");
            given(tokenService.getExpirationSeconds()).willReturn(28800L);

            LoginResponse response = authService.login(new LoginCommand("testuser", "correctpass"));

            assertThat(response.token()).isEqualTo("jwt-token-abc");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(28800L);
        }

        @Test
        @DisplayName("throws when username not found or password is wrong")
        void throwsForBadCredentials() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(new LoginCommand("nobody", "pass")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("throws when account is deactivated")
        void throwsForDeactivatedAccount() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new DisabledException("disabled"));

            assertThatThrownBy(() -> authService.login(new LoginCommand("testuser", "pass")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("deactivated");
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("invalidates the token in the blacklist")
        void invalidatesToken() {
            authService.logout("token-xyz");
            then(tokenBlacklist).should().invalidate("token-xyz");
        }
    }
}
