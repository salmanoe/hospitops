package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.HotelLoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.domain.port.out.HotelStatusPort;
import id.co.hospitops.identity.domain.port.out.RefreshTokenStore;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.identity.domain.port.out.TokenService;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelAuthService")
class HotelAuthServiceTest {

    @Mock
    HotelStatusPort hotelStatusPort;
    @Mock
    StaffRepository staffRepository;
    @Mock
    TokenService tokenService;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    RefreshTokenStore refreshTokenStore;

    HotelAuthService service;

    private static final UUID HOTEL_UUID = UUID.randomUUID();
    private static final HotelId HOTEL_ID = HotelId.of(HOTEL_UUID);
    private static final String USERNAME = "alice";
    private static final String RAW_PASSWORD = "secret";
    private static final String HASH = "$2a$10$hash";

    @BeforeEach
    void setUp() {
        service = new HotelAuthService(
                staffRepository, hotelStatusPort, tokenService,
                passwordEncoder, refreshTokenStore, 604_800L);
    }

    private Staff activeStaff() {
        return Staff.reconstitute(
                StaffId.generate(), "Alice Smith", USERNAME, HASH,
                StaffRole.FRONT_DESK, true, HOTEL_ID,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("succeeds when hotel is ACTIVE, staff exists and password matches")
        void success() {
            Staff staff = activeStaff();
            given(hotelStatusPort.isActive(HOTEL_ID)).willReturn(true);
            given(staffRepository.findByUsernameAndHotelId(USERNAME, HOTEL_ID))
                    .willReturn(Optional.of(staff));
            given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(true);
            given(tokenService.generate(staff)).willReturn("access-token");
            given(tokenService.getExpirationSeconds()).willReturn(28_800L);

            LoginResponse response = service.login(new HotelLoginCommand(HOTEL_UUID, USERNAME, RAW_PASSWORD));

            assertThat(response.token()).isEqualTo("access-token");
            assertThat(response.username()).isEqualTo(USERNAME);
            then(refreshTokenStore).should().store(org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.eq(staff.getId()),
                    org.mockito.ArgumentMatchers.eq(604_800L));
        }

        @Test
        @DisplayName("rejects when hotel is not ACTIVE")
        void rejectsInactiveHotel() {
            given(hotelStatusPort.isActive(HOTEL_ID)).willReturn(false);

            assertThatThrownBy(() ->
                    service.login(new HotelLoginCommand(HOTEL_UUID, USERNAME, RAW_PASSWORD)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("not currently active");

            then(staffRepository).should(never())
                    .findByUsernameAndHotelId(org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("rejects when staff not found in that hotel")
        void rejectsUnknownStaff() {
            given(hotelStatusPort.isActive(HOTEL_ID)).willReturn(true);
            given(staffRepository.findByUsernameAndHotelId(USERNAME, HOTEL_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.login(new HotelLoginCommand(HOTEL_UUID, USERNAME, RAW_PASSWORD)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("rejects inactive staff account")
        void rejectsDeactivatedStaff() {
            Staff inactive = Staff.reconstitute(
                    StaffId.generate(), "Alice Smith", USERNAME, HASH,
                    StaffRole.FRONT_DESK, false /* inactive */, HOTEL_ID,
                    LocalDateTime.now(), LocalDateTime.now());

            given(hotelStatusPort.isActive(HOTEL_ID)).willReturn(true);
            given(staffRepository.findByUsernameAndHotelId(USERNAME, HOTEL_ID))
                    .willReturn(Optional.of(inactive));

            assertThatThrownBy(() ->
                    service.login(new HotelLoginCommand(HOTEL_UUID, USERNAME, RAW_PASSWORD)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("rejects when password does not match")
        void rejectsBadPassword() {
            given(hotelStatusPort.isActive(HOTEL_ID)).willReturn(true);
            given(staffRepository.findByUsernameAndHotelId(USERNAME, HOTEL_ID))
                    .willReturn(Optional.of(activeStaff()));
            given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(false);

            assertThatThrownBy(() ->
                    service.login(new HotelLoginCommand(HOTEL_UUID, USERNAME, RAW_PASSWORD)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Invalid username or password");
        }
    }
}
