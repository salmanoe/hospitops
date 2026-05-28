package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.LoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.domain.port.out.RefreshTokenStore;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenBlacklist;
import id.co.hospitops.identity.domain.port.out.TokenService;
import id.co.hospitops.identity.infrastructure.security.StaffUserDetails;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock TokenService          tokenService;
    @Mock TokenBlacklist        tokenBlacklist;
    @Mock RefreshTokenStore     refreshTokenStore;
    @Mock StaffRepository       staffRepository;
    @Mock Authentication        authentication;

    /** Use constructor injection so we can supply the refresh TTL directly. */
    AuthService authService;

    private static final long REFRESH_TTL_SECONDS = 604_800L; // 7 days

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager, tokenService, tokenBlacklist,
                refreshTokenStore, staffRepository, REFRESH_TTL_SECONDS);
    }

    private Staff activeStaff() {
        return Staff.reconstitute(StaffId.generate(), "Test User", "testuser",
                "hashed", StaffRole.FRONT_DESK, true, LocalDateTime.now(), LocalDateTime.now());
    }

    private Staff inactiveStaff() {
        return Staff.reconstitute(StaffId.generate(), "Inactive", "inactive",
                "hashed", StaffRole.FRONT_DESK, false, LocalDateTime.now(), LocalDateTime.now());
    }

    // ══════════════════════════════════════════════════════════════
    // login()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns access token and refresh token when credentials are valid")
        void returnsTokensForValidCredentials() {
            Staff staff = activeStaff();
            given(authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken("testuser", "correctpass")))
                    .willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(new StaffUserDetails(staff));
            given(tokenService.generate(staff)).willReturn("access-jwt");
            given(tokenService.getExpirationSeconds()).willReturn(28800L);

            LoginResponse response = authService.login(new LoginCommand("testuser", "correctpass"));

            assertThat(response.token()).isEqualTo("access-jwt");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(28800L);
            assertThat(response.refreshToken()).isNotNull().isNotBlank();
            assertThat(response.refreshExpiresIn()).isEqualTo(REFRESH_TTL_SECONDS);
        }

        @Test
        @DisplayName("stores the refresh token in RefreshTokenStore with correct TTL")
        void storesRefreshTokenInStore() {
            Staff staff = activeStaff();
            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(new StaffUserDetails(staff));
            given(tokenService.generate(any())).willReturn("access-jwt");
            given(tokenService.getExpirationSeconds()).willReturn(28800L);

            LoginResponse response = authService.login(new LoginCommand("testuser", "pass"));

            then(refreshTokenStore).should().store(
                    eq(response.refreshToken()),
                    eq(staff.getId()),
                    eq(REFRESH_TTL_SECONDS));
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException for bad credentials")
        void throwsForBadCredentials() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(new LoginCommand("nobody", "pass")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException for deactivated account")
        void throwsForDeactivatedAccount() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new DisabledException("disabled"));

            assertThatThrownBy(() -> authService.login(new LoginCommand("testuser", "pass")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("deactivated");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // refresh()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("issues new access + refresh tokens and rotates the old refresh token")
        void issuesNewTokensAndRotates() {
            Staff staff = activeStaff();
            String oldRefresh = "old-refresh-uuid";

            given(refreshTokenStore.findStaffId(oldRefresh)).willReturn(Optional.of(staff.getId()));
            given(staffRepository.findById(staff.getId())).willReturn(Optional.of(staff));
            given(tokenService.generate(staff)).willReturn("new-access-jwt");
            given(tokenService.getExpirationSeconds()).willReturn(28800L);

            LoginResponse response = authService.refresh(oldRefresh);

            assertThat(response.token()).isEqualTo("new-access-jwt");
            assertThat(response.refreshToken()).isNotNull().isNotEqualTo(oldRefresh);
            assertThat(response.refreshExpiresIn()).isEqualTo(REFRESH_TTL_SECONDS);

            // Old token revoked before new one stored
            InOrder inOrder = inOrder(refreshTokenStore);
            inOrder.verify(refreshTokenStore).revoke(oldRefresh);
            inOrder.verify(refreshTokenStore).store(
                    eq(response.refreshToken()), eq(staff.getId()), eq(REFRESH_TTL_SECONDS));
        }

        @Test
        @DisplayName("throws when refresh token is unknown or expired")
        void throwsForUnknownRefreshToken() {
            given(refreshTokenStore.findStaffId("bad-token")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("bad-token"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("invalid or expired");

            verifyNoInteractions(staffRepository, tokenService);
        }

        @Test
        @DisplayName("throws when the owning staff member no longer exists")
        void throwsWhenStaffNotFound() {
            StaffId staffId = StaffId.generate();
            given(refreshTokenStore.findStaffId("token")).willReturn(Optional.of(staffId));
            given(staffRepository.findById(staffId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("token"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(tokenService);
        }

        @Test
        @DisplayName("throws when the owning staff member is inactive")
        void throwsForInactiveStaff() {
            Staff inactive = inactiveStaff();
            given(refreshTokenStore.findStaffId("token")).willReturn(Optional.of(inactive.getId()));
            given(staffRepository.findById(inactive.getId())).willReturn(Optional.of(inactive));

            assertThatThrownBy(() -> authService.refresh("token"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("deactivated");

            verifyNoInteractions(tokenService);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // logout()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("blacklists the access token")
        void blacklistsAccessToken() {
            authService.logout("access-token", null);
            then(tokenBlacklist).should().invalidate("access-token");
        }

        @Test
        @DisplayName("also revokes the refresh token when provided")
        void revokesRefreshTokenWhenProvided() {
            authService.logout("access-token", "refresh-token");
            then(tokenBlacklist).should().invalidate("access-token");
            then(refreshTokenStore).should().revoke("refresh-token");
        }

        @Test
        @DisplayName("does NOT call RefreshTokenStore when refreshToken is null")
        void skipsRefreshRevocationWhenNull() {
            authService.logout("access-token", null);
            then(refreshTokenStore).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("does NOT call RefreshTokenStore when refreshToken is blank")
        void skipsRefreshRevocationWhenBlank() {
            authService.logout("access-token", "   ");
            then(refreshTokenStore).shouldHaveNoInteractions();
        }
    }
}
