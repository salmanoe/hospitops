package id.co.hospitops.group.application;

import id.co.hospitops.group.application.command.GroupLoginCommand;
import id.co.hospitops.group.application.response.GroupLoginResponse;
import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.group.domain.port.out.GroupAdminRepository;
import id.co.hospitops.group.domain.port.out.GroupTokenService;
import id.co.hospitops.group.domain.port.out.HotelLookupPort;
import id.co.hospitops.group.domain.port.out.HotelLookupPort.HotelAccessResult;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupAuthService")
class GroupAuthServiceTest {

    @Mock
    GroupAdminRepository adminRepository;
    @Mock
    GroupTokenService tokenService;
    @Mock
    HotelLookupPort hotelLookupPort;
    @Mock
    PasswordEncoder passwordEncoder;

    GroupAuthService service;

    private static final String EMAIL = "admin@acme.com";
    private static final String RAW_PASSWORD = "pass123";
    private static final String HASH = "$2a$10$hash";
    private static final GroupId GROUP_ID = GroupId.generate();
    private static final HotelId HOTEL_ID = HotelId.generate();
    private static final GroupAdminId ADMIN_ID = GroupAdminId.generate();

    @BeforeEach
    void setUp() {
        service = new GroupAuthService(adminRepository, tokenService, hotelLookupPort, passwordEncoder);
    }

    private GroupAdmin admin() {
        return GroupAdmin.reconstitute(ADMIN_ID, GROUP_ID, EMAIL, HASH,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("issues a group-scoped token on valid credentials")
        void success() {
            given(adminRepository.findByEmail(EMAIL)).willReturn(Optional.of(admin()));
            given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(true);
            given(tokenService.issueGroupToken(ADMIN_ID, GROUP_ID, EMAIL))
                    .willReturn("group-token");
            given(tokenService.getExpirationSeconds()).willReturn(28_800L);

            GroupLoginResponse response = service.login(new GroupLoginCommand(EMAIL, RAW_PASSWORD));

            assertThat(response.accessToken()).isEqualTo("group-token");
            assertThat(response.hotelId()).isNull();
            assertThat(response.groupId()).isEqualTo(GROUP_ID);
        }

        @Test
        @DisplayName("rejects unknown email")
        void unknownEmail() {
            given(adminRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.login(new GroupLoginCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("rejects wrong password")
        void wrongPassword() {
            given(adminRepository.findByEmail(EMAIL)).willReturn(Optional.of(admin()));
            given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(false);

            assertThatThrownBy(() -> service.login(new GroupLoginCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("enterHotel")
    class EnterHotel {

        private final Instant expiry = Instant.now().plusSeconds(3600);
        private final String rawGroupToken = "raw-group-token";
        private final GroupAdminPrincipal principal =
                new GroupAdminPrincipal(ADMIN_ID, GROUP_ID, EMAIL, null);

        @Test
        @DisplayName("issues a hotel-scoped token when hotel belongs to group and is ACTIVE")
        void success() {
            given(hotelLookupPort.verifyAccess(HOTEL_ID, GROUP_ID)).willReturn(HotelAccessResult.ALLOWED);
            given(tokenService.parseExpiry(rawGroupToken)).willReturn(expiry);
            given(tokenService.issueHotelToken(ADMIN_ID, GROUP_ID, EMAIL, HOTEL_ID, expiry))
                    .willReturn("hotel-token");
            given(tokenService.getExpirationSeconds()).willReturn(3600L);

            GroupLoginResponse response = service.enterHotel(principal, HOTEL_ID, rawGroupToken);

            assertThat(response.accessToken()).isEqualTo("hotel-token");
            assertThat(response.hotelId()).isEqualTo(HOTEL_ID);
        }

        @Test
        @DisplayName("rejects when hotel does not belong to group")
        void wrongGroup() {
            given(hotelLookupPort.verifyAccess(HOTEL_ID, GROUP_ID))
                    .willReturn(HotelAccessResult.NOT_FOUND_OR_WRONG_GROUP);

            assertThatThrownBy(() -> service.enterHotel(principal, HOTEL_ID, rawGroupToken))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("does not belong to your group");

            then(tokenService).should(never()).issueHotelToken(ADMIN_ID, GROUP_ID, EMAIL, HOTEL_ID, expiry);
        }

        @Test
        @DisplayName("issues a hotel-scoped token when hotel is in SETUP status (setup wizard flow)")
        void setupHotelAllowed() {
            // SETUP hotels return ALLOWED so GROUP_ADMIN can complete the setup wizard
            given(hotelLookupPort.verifyAccess(HOTEL_ID, GROUP_ID)).willReturn(HotelAccessResult.ALLOWED);
            given(tokenService.parseExpiry(rawGroupToken)).willReturn(expiry);
            given(tokenService.issueHotelToken(ADMIN_ID, GROUP_ID, EMAIL, HOTEL_ID, expiry))
                    .willReturn("setup-hotel-token");
            given(tokenService.getExpirationSeconds()).willReturn(3600L);

            GroupLoginResponse response = service.enterHotel(principal, HOTEL_ID, rawGroupToken);

            assertThat(response.accessToken()).isEqualTo("setup-hotel-token");
            assertThat(response.hotelId()).isEqualTo(HOTEL_ID);
        }

        @Test
        @DisplayName("rejects when hotel is SUSPENDED")
        void suspendedHotel() {
            given(hotelLookupPort.verifyAccess(HOTEL_ID, GROUP_ID))
                    .willReturn(HotelAccessResult.SUSPENDED);

            assertThatThrownBy(() -> service.enterHotel(principal, HOTEL_ID, rawGroupToken))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("suspended");

            then(tokenService).should(never()).issueHotelToken(ADMIN_ID, GROUP_ID, EMAIL, HOTEL_ID, expiry);
        }
    }
}
